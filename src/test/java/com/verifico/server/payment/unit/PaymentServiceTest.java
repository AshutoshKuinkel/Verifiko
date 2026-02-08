// These tests only cover some core business logic for the payment API,
// the integration tests will have to be done at great depth....

package com.verifico.server.payment.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.payment.CreditsPurchasedAmount;
import com.verifico.server.payment.Payment;
import com.verifico.server.payment.PaymentRepository;
import com.verifico.server.payment.PaymentService;
import com.verifico.server.payment.dto.PaymentIntentResponse;
import com.verifico.server.payment.dto.PurchaseCreditsRequest;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock
  PaymentRepository paymentRepository;

  @Mock
  UserRepository userRepository;

  @Mock
  StringRedisTemplate redisTemplate;

  @Mock
  ValueOperations<String, String> valueOperations;

  @Mock
  SecurityContext securityContext;

  @Mock
  Authentication authentication;

  @InjectMocks
  PaymentService paymentService;

  @BeforeEach
  void setup() {
    SecurityContextHolder.setContext(securityContext);
    ReflectionTestUtils.setField(paymentService, "stripeKey", "sk_test_fake_key");
  }

  private User mockUser() {
    User user = new User();
    user.setId(1L);
    user.setUsername("JohnDoe123");
    user.setEmail("johndoe2@gmail.com");
    user.setPassword("hashedPass");
    return user;
  }

  private PurchaseCreditsRequest validPurchaseRequest() {
    PurchaseCreditsRequest request = new PurchaseCreditsRequest();
    request.setAmount(CreditsPurchasedAmount.BUY_25_CREDITS);
    request.setQuantity(1);
    request.setCurrency("usd");
    return request;
  }

  // tests for create-payment-intent API:
  // 1. Authentication failed (no auth provided)
  @Test
  void authenticationFailedWhenCreatingPaymentIntent() {
    when(securityContext.getAuthentication()).thenReturn(null);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> paymentService.paymentIntent(null, "idempotency-key"));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertEquals("Authenticated user not found!", ex.getReason());

    verify(paymentRepository, never()).save(any());
  }

  // 2. Authenticated user not found in DB
  @Test
  void authenticatedUserNotFoundInDB() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");
    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> paymentService.paymentIntent(null, "idempotency-key"));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("User not found!", ex.getReason());

    verify(paymentRepository, never()).save(any());
  }

  // 3. Request validation failed: invalid 'quantity' (>1)
  @Test
  void invalidQuantityWhenCreatingPaymentIntent() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    User user = mockUser();
    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.of(user));

    PurchaseCreditsRequest request = validPurchaseRequest();
    request.setQuantity(5);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> paymentService.paymentIntent(request, "idempotency-key"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Quantity must be 1", ex.getReason());

    verify(paymentRepository, never()).save(any());
  }

  // 4. Idempotency key reuse: return cached clientSecret and paymentIntentId
  @Test
  void idempotencyKeyReusesCache() throws Exception {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    User user = mockUser();
    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.of(user));

    String cachedValue = "pi_test_secret,pi_test_id";

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("payment_idempotency:cached-key")).thenReturn(cachedValue);

    PaymentIntentResponse response = paymentService.paymentIntent(validPurchaseRequest(), "cached-key");

    assertNotNull(response);
    assertEquals("pi_test_secret", response.clientSecret());
    assertEquals("pi_test_id", response.paymentIntentId());

    verify(paymentRepository, never()).save(any());
  }

  // 5. Redis cache contains malformed value (missing comma) ->
  // ArrayIndexOutOfBoundsException
  @Test
  void malformedRedisCacheMissingComma() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    User user = mockUser();
    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.of(user));

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("payment_idempotency:bad-key")).thenReturn("malformed_value_no_comma");

    PurchaseCreditsRequest request = validPurchaseRequest();

    assertThrows(ArrayIndexOutOfBoundsException.class,
        () -> paymentService.paymentIntent(request, "bad-key"));
  }

  // Helper method tests:
  // 6. getAmountBasedOnCreditsPurchasedInCents: BUY_25_CREDITS returns 399L
  @Test
  void getAmountFor25Credits() {
    long amount = paymentService.getAmountBasedOnCreditsPurchasedInCents(CreditsPurchasedAmount.BUY_25_CREDITS);
    assertEquals(399L, amount);
  }

  // 7. getAmountBasedOnCreditsPurchasedInCents: BUY_50_CREDITS returns 799L
  @Test
  void getAmountFor50Credits() {
    long amount = paymentService.getAmountBasedOnCreditsPurchasedInCents(CreditsPurchasedAmount.BUY_50_CREDITS);
    assertEquals(799L, amount);
  }

  // 8. getAmountBasedOnCreditsPurchasedInCents: BUY_75_CREDITS returns 999L
  @Test
  void getAmountFor75Credits() {
    long amount = paymentService.getAmountBasedOnCreditsPurchasedInCents(CreditsPurchasedAmount.BUY_75_CREDITS);
    assertEquals(999L, amount);
  }

  // 9. getAmountBasedOnCreditsPurchasedInCents: BUY_150_CREDITS returns 1499L
  @Test
  void getAmountFor150Credits() {
    long amount = paymentService.getAmountBasedOnCreditsPurchasedInCents(CreditsPurchasedAmount.BUY_150_CREDITS);
    assertEquals(1499L, amount);
  }

  // 10. toTransaction: verify Payment entity is created with correct fields
  @Test
  void toTransactionCreatesPaymentCorrectly() {
    User user = mockUser();
    Payment savedPayment = new Payment();
    savedPayment.setPaymentIntentId("pi_test_123");
    savedPayment.setPurchasedPackage(CreditsPurchasedAmount.BUY_25_CREDITS);
    savedPayment.setTransactionInitiator(user);
    savedPayment.setAmountInCents(399L);

    when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

    Payment result = paymentService.toTransaction("pi_test_123", CreditsPurchasedAmount.BUY_25_CREDITS, user, 399L);

    assertNotNull(result);
    assertEquals("pi_test_123", result.getPaymentIntentId());
    assertEquals(CreditsPurchasedAmount.BUY_25_CREDITS, result.getPurchasedPackage());
    assertEquals(user, result.getTransactionInitiator());
    assertEquals(399L, result.getAmountInCents());

    verify(paymentRepository, times(1)).save(any(Payment.class));
  }

  // 11. toTransaction: verify paymentRepository.save() is called
  @Test
  void toTransactionCallsRepositorySave() {
    User user = mockUser();
    Payment savedPayment = new Payment();

    when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

    paymentService.toTransaction("pi_test_123", CreditsPurchasedAmount.BUY_25_CREDITS, user, 399L);

    verify(paymentRepository, times(1)).save(any(Payment.class));
  }
}
