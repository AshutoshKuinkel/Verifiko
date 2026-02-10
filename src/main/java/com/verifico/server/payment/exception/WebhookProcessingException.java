package com.verifico.server.payment.exception;

public class WebhookProcessingException extends RuntimeException {

  public WebhookProcessingException(String message) {
    super(message);
  }

  public WebhookProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
