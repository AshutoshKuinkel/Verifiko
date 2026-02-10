package com.verifico.server.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.StripeException;
import com.verifico.server.common.dto.APIResponse;
import com.verifico.server.payment.dto.PaymentIntentResponse;
import com.verifico.server.payment.dto.PurchaseCreditsRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/payment-intent")
  public ResponseEntity<APIResponse<PaymentIntentResponse>> createPaymentIntent(
      @Valid @RequestBody PurchaseCreditsRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey) throws StripeException {

    PaymentIntentResponse response = paymentService.paymentIntent(request, idempotencyKey);

    return ResponseEntity.status(HttpStatus.CREATED.value())
        .body(new APIResponse<>("Payment Intent Successfully Created", response));
  }

  @PostMapping("/webhook/stripe")
  public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
      @RequestHeader("Stripe-Signature") String sigHeader) {
    try {
      paymentService.processWebhook(payload, sigHeader);
      return ResponseEntity.ok("");
    } catch (SecurityException e) {
      // Invallid signature = permanent failure,
      // doesn't matter if we retry as outcome will be same...
      log.error("Webhook security violation", e);
      return ResponseEntity.ok("");
    } catch (Exception e) {
      // everything else retry for, as it could be transistent
      // i think we can also refactor the way we handle retries
      // specifically regarding catching what type of err we are
      // encountering to optimise @ scale... but this should be
      // fine to handle quite a bit of users without any problems.
      log.error("Webhook processing failed, will retry", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("null");
    }
  }
}
