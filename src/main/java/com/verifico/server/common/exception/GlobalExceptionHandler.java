package com.verifico.server.common.exception;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
    ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), ex.getStatusCode().value(),
        ex.getStatusCode().toString(), ex.getReason());
    return new ResponseEntity<>(errorResponse, ex.getStatusCode());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), 500, "Internal Server Error", ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMissingBody(HttpMessageNotReadableException ex) {
    String errorMessage = "Request body is missing or invalid";
    if (ex.getCause() instanceof InvalidFormatException) {
      InvalidFormatException ife = (InvalidFormatException) ex.getCause();
      if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
        errorMessage = "Invalid enum value provided";
      }
    }
    ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), 400, "Bad request", errorMessage);
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
    String errorMessage = ex.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> fieldError.getDefaultMessage())
        .findFirst()
        .orElse("Invalid Request");

    ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), 400, "Validation Error", errorMessage);
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    String errorMessage = ex.getConstraintViolations().stream()
        .map(v -> v.getMessage())
        .findFirst()
        .orElse("Invalid Request Parameter");

    ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now(), 400, "Validation Error", errorMessage);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {

    ErrorResponse errorResponse = new ErrorResponse(
        LocalDateTime.now(),
        HttpStatus.CONFLICT.value(),
        "Conflict",
        "Resource already exists");

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }
}
