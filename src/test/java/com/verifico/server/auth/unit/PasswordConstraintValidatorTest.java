package com.verifico.server.auth.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.verifico.server.auth.validation.PasswordConstraintValidator;

import jakarta.validation.ConstraintValidatorContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PasswordConstraintValidatorTest {

  private PasswordConstraintValidator validator;
  private ConstraintValidatorContext context;

  @BeforeEach
  void setUp() {
    validator = new PasswordConstraintValidator();
    context = mock(ConstraintValidatorContext.class);
    when(context.buildConstraintViolationWithTemplate(anyString()))
        .thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.class));
  }

  @Test
  void validPassword_shouldPass() {
    assertTrue(validator.isValid("Password123!", context));
  }

  @Test
  void passwordTooShort_shouldFail() {
    assertFalse(validator.isValid("Pass1!", context));
  }

  @Test
  void passwordNoUppercase_shouldFail() {
    assertFalse(validator.isValid("password123!", context));
  }

  @Test
  void passwordNoLowercase_shouldFail() {
    assertFalse(validator.isValid("PASSWORD123!", context));
  }

  @Test
  void passwordNoDigit_shouldFail() {
    assertFalse(validator.isValid("Password!", context));
  }

  @Test
  void passwordNoSpecialChar_shouldFail() {
    assertFalse(validator.isValid("Password123", context));
  }

  @Test
  void passwordWithWhitespace_shouldFail() {
    assertFalse(validator.isValid("Pass word123!", context));
  }

  @Test
  void nullPassword_shouldFail() {
    // Null validation is handled by @NotBlank, not @ValidPassword
    assertTrue(validator.isValid(null, context));
  }
}