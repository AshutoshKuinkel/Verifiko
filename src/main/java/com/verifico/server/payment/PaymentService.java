package com.verifico.server.payment;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.verifico.server.credit.CreditService;
import com.verifico.server.payment.dto.PaymentIntentResponse;
import com.verifico.server.payment.dto.PurchaseCreditsRequest;
import com.verifico.server.payment.exception.WebhookProcessingException;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

  @Value("${stripe.secret-key}")
  private String stripeKey;

  @Value("${stripe.webhook-secret}")
  private String webhookSecret;

  private final UserRepository userRepository;

  private final PaymentRepository paymentRepository;

  private final StringRedisTemplate redisTemplate;

  private final CreditService creditService;

  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

  @Transactional
  public PaymentIntentResponse paymentIntent(PurchaseCreditsRequest request, String idempotencyKey)
      throws StripeException {

    // to optimise this function further look into preventing double network calls,
    // e.g if user double clicks pay now very fast then redis will show empty cache
    // and stripe will get called twice, it's nothing major but can be optimised.

    // is user logged in first
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found!");
    }
    String username = auth.getName();

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));

    CreditsPurchasedAmount purchasedAmount = request.getAmount();

    if (request.getQuantity() != 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be 1");
    }

    Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent() && existing.get().getPaymentIntentId() != null) {
      String paymentIntentId = existing.get().getPaymentIntentId();

      String redisKey = "payment_idempotency:" + idempotencyKey;
      String cachedValue = redisTemplate.opsForValue().get(redisKey);
      if (cachedValue != null) {
        String[] parts = cachedValue.split(",");
        return new PaymentIntentResponse(parts[0], parts[1]);
      }

      // If not in Redis, retrieve from Stripe
      RequestOptions retrieveOptions = RequestOptions.builder()
          .setApiKey(stripeKey)
          .build();
      PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId, retrieveOptions); // ← FIXED

      // Re-cache it
      String valueToCache = intent.getClientSecret() + "," + intent.getId();
      redisTemplate.opsForValue().set(redisKey, valueToCache, Duration.ofMinutes(15));

      return new PaymentIntentResponse(
          intent.getClientSecret(),
          intent.getId());
    }

    // check cache for secret key/pi_id:
    String redisKey = "payment_idempotency:" + idempotencyKey;
    String cachedValue = redisTemplate.opsForValue().get(redisKey);

    if (cachedValue != null) {
      // returning without going to db/stripe,
      // our info is stored in client_secret,pi_id format
      String[] parts = cachedValue.split(",");
      return new PaymentIntentResponse(parts[0], parts[1]);
    }

    // create payment intent with user_id,pkg_id metadata + stripe idempotency
    // header from session storage
    Long amountInCents = getAmountBasedOnCreditsPurchasedInCents(purchasedAmount);

    PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
        .setAmount(amountInCents)
        .setCurrency(request.getCurrency())
        .setAutomaticPaymentMethods(
            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                .setEnabled(true)
                // we only have this .setAllowRedirects set to never for local testing
                // remove this entire:
                // .setAllowRedirects(
                // PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                // once we connect to frontend
                .setAllowRedirects(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                .build())
        .putMetadata("user_id", user.getId().toString())
        .putMetadata("purchase_type", purchasedAmount.name())
        .build();

    // calling stripe w idempotency keym helps is redis fails but stripe succeeds:
    RequestOptions options = RequestOptions.builder()
        .setApiKey(stripeKey)
        .setIdempotencyKey(idempotencyKey)
        .build();

    PaymentIntent paymentIntent = PaymentIntent.create(params, options);

    // make sure payment status is set to pending with payment intent id
    toTransaction(idempotencyKey, paymentIntent.getId(), purchasedAmount, user, amountInCents);

    // set client secret key with ttl 15 mins in redis cache.
    String valueToCache = paymentIntent.getClientSecret() + "," + paymentIntent.getId();
    redisTemplate.opsForValue().set(redisKey, valueToCache, Duration.ofMinutes(15));

    // return client secret + payment intent id not entire payment intent (best
    // practice)
    return new PaymentIntentResponse(
        paymentIntent.getClientSecret(),
        paymentIntent.getId());
  }

  public void processWebhook(String payload, String sigHeader) {
    try {
      Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
      switch (event.getType()) {

        case "payment_intent.succeeded" -> {
          Optional<StripeObject> obj = event.getDataObjectDeserializer().getObject();

          if (obj.isEmpty()) {
            log.warn(
                "Unable to deserialize Stripe event {} (type: {}, api version: {}) — possibly API version mismatch",
                event.getId(), event.getType(), event.getApiVersion());
            return; // acknowlegde only, don't retry
          }

          PaymentIntent intent = (PaymentIntent) obj.get();

          // log successful payment along with intent id here
          log.info("✔ Payment successful: {}", intent.getId());
          // call handleSuccessfulPayment function here
          handleSuccessPayment(intent.getId());
        }

        case "payment_intent.payment_failed" -> {
          Optional<StripeObject> obj = event.getDataObjectDeserializer().getObject();

          if (obj.isEmpty()) {
            log.warn(
                "Unable to deserialize Stripe event {} (type: {}, api version: {}) — possibly API version mismatch",
                event.getId(), event.getType(), event.getApiVersion());
            return; // acknowlegde only, don't retry
          }

          PaymentIntent intent = (PaymentIntent) obj.get();

          // log payment failed w intent id here
          log.warn("✘ Payment failed: {}", intent.getId());
          // call handlefailedpayment function here
          handleFailedPayment(intent.getId());
        }

        // if not either of these 2 above, log unhandled event type. (default)
        default -> log.debug("Unhandled event type: {}", event.getType());
      }
    } catch (SignatureVerificationException e) {
      // log invalid webhook signature
      log.error("✘ Invalid webhook signature", e);
      throw new SecurityException("Invalid webhook signature");
    } catch (Exception e) {
      // log webhook processing err
      log.error("✘ Webhook processing error", e);
      throw new WebhookProcessingException("Unexpected webhook processing failure", e);
    }
  }

  @Transactional
  private void handleSuccessPayment(String paymentIntentId) {
    Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

    // check if payment already processed:
    if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
      return;
    }

    if (payment.getCreditsAwarded() != null && payment.getCreditsAwarded()) {
      log.info("Credits already awarded for payment {}, skipping", payment.getId());
      return;
    }

    // adding credits to user
    int credits = switch (payment.getPurchasedPackage()) {
      case BUY_25_CREDITS -> 25;
      case BUY_50_CREDITS -> 50;
      case BUY_75_CREDITS -> 75;
      case BUY_150_CREDITS -> 150;
    };

    creditService.addPurchasedCredits(payment.getTransactionInitiator().getId(), credits);
    log.info("{} credits successfully added to user {} balance", credits, payment.getTransactionInitiator().getId());

    payment.setStatus(PaymentStatus.SUCCEEDED);
    payment.setCreditsAwarded(true);
    paymentRepository.save(payment);
    log.info("Payment {} marked as successful", payment.getId());
  }

  @Transactional
  private void handleFailedPayment(String paymentIntentId) {
    paymentRepository.findByPaymentIntentId(paymentIntentId)
        .ifPresentOrElse(payment -> {
          if (payment.getStatus() != PaymentStatus.FAILED) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Payment {} marked as failed", payment.getId());
          }
        }, () -> log.warn("Payment with intent {} not found, ignoring", paymentIntentId));
  }

  // helpers:
  public long getAmountBasedOnCreditsPurchasedInCents(CreditsPurchasedAmount purchasedAmount) {
    return switch (purchasedAmount) {
      case BUY_25_CREDITS -> 399L;
      case BUY_50_CREDITS -> 799L;
      case BUY_75_CREDITS -> 999L;
      case BUY_150_CREDITS -> 1499L;
    };
  }

  public Payment toTransaction(String idempotencyKey, String paymentIntentId, CreditsPurchasedAmount purchasePackage,
      User transactionInitiator, Long amount) {
    Payment transaction = new Payment();
    transaction.setIdempotencyKey(idempotencyKey);
    transaction.setPaymentIntentId(paymentIntentId);
    transaction.setPurchasedPackage(purchasePackage);
    transaction.setTransactionInitiator(transactionInitiator);
    transaction.setAmountInCents(amount);

    paymentRepository.save(transaction);
    return transaction;
  }
}
