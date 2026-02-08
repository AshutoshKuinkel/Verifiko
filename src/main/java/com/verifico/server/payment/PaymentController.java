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

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
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
}
