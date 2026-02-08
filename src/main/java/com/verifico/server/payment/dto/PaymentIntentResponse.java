package com.verifico.server.payment.dto;

public record PaymentIntentResponse(
    String clientSecret,
    String paymentIntentId) {
}
