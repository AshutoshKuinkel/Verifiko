package com.verifico.server.payment;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.verifico.server.payment.dto.PaymentIntentResponse;
import com.verifico.server.payment.dto.PurchaseCreditsRequest;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

  @Value("${stripe.secret-key}")
  private String stripeKey;

  private final UserRepository userRepository;

  private final PaymentRepository paymentRepository;

  private final StringRedisTemplate redisTemplate;

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

    // check cache for secret key/pi_id first:
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
    toTransaction(paymentIntent.getId(), purchasedAmount, user, amountInCents);

    // set client secret key with ttl 15 mins in redis cache.
    String valueToCache = paymentIntent.getClientSecret() + "," + paymentIntent.getId();
    redisTemplate.opsForValue().set(redisKey, valueToCache, Duration.ofMinutes(15));

    // return client secret + payment intent id not entire payment intent (best
    // practice)
    return new PaymentIntentResponse(
        paymentIntent.getClientSecret(),
        paymentIntent.getId());
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

  public Payment toTransaction(String paymentIntentId, CreditsPurchasedAmount purchasePackage,
      User transactionInitiator, Long amount) {
    Payment transaction = new Payment();
    transaction.setPaymentIntentId(paymentIntentId);
    transaction.setPurchasedPackage(purchasePackage);
    transaction.setTransactionInitiator(transactionInitiator);
    transaction.setAmountInCents(amount);

    paymentRepository.save(transaction);
    return transaction;
  }
}
