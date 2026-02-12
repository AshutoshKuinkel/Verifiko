// These tests only cover some core business logic for the payment API,
// the integration tests will have to be done at great depth....

package com.verifico.server.payment.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.verifico.server.credit.CreditService;
import com.verifico.server.payment.CreditsPurchasedAmount;
import com.verifico.server.payment.Payment;
import com.verifico.server.payment.PaymentRepository;
import com.verifico.server.payment.PaymentService;
import com.verifico.server.payment.PaymentStatus;
import com.verifico.server.payment.dto.PaymentIntentResponse;
import com.verifico.server.payment.dto.PurchaseCreditsRequest;
import com.verifico.server.payment.exception.WebhookProcessingException;
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
  CreditService creditService;

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
    ReflectionTestUtils.setField(paymentService, "webhookSecret", "whsec_fake_secret_for_tests");
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

    Payment result = paymentService.toTransaction("idempotency-key-123", "pi_test_123",
        CreditsPurchasedAmount.BUY_25_CREDITS, user, 399L);

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

    paymentService.toTransaction("idempotency-key-123", "pi_test_123", CreditsPurchasedAmount.BUY_25_CREDITS, user,
        399L);

    verify(paymentRepository, times(1)).save(any(Payment.class));
  }

  // webhook core buisness logic test endpoints:
  // 1. processWebhook_succeeded_happyPath_awardsCorrectCredits()
  @Test
  void processWebhook_succeeded_happyPath_awardsCorrectCredits() {
    // Creating try-with-resources block for mockStatic
    try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {

      // mock stripe event
      Event mockEvent = mock(Event.class);
      PaymentIntent mockIntent = mock(PaymentIntent.class);
      EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

      when(mockIntent.getId()).thenReturn("pi_test_123");
      when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
      when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
      when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));

      // mock static Webhook.constructEvent
      webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
          .thenReturn(mockEvent);

      User user = mockUser();
      Payment payment = new Payment();
      payment.setId(42L);
      payment.setPaymentIntentId("pi_test_123");
      payment.setPurchasedPackage(CreditsPurchasedAmount.BUY_50_CREDITS);
      payment.setTransactionInitiator(user);
      payment.setStatus(PaymentStatus.PENDING);
      payment.setCreditsAwarded(false);

      when(paymentRepository.findByPaymentIntentId("pi_test_123"))
          .thenReturn(Optional.of(payment));

      paymentService.processWebhook("fake-payload", "fake-signature");

      assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
      assertTrue(payment.getCreditsAwarded());

      verify(paymentRepository).save(payment);
      verify(creditService).addPurchasedCredits(user.getId(), 50);
    }
  }

  // 2. processWebhook_succeeded_alreadySucceeded_doesNothing()
  @Test
  void processWebhook_succeeded_alreadySucceeded_doesNothing() {
    try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {

      Event mockEvent = mock(Event.class);
      PaymentIntent mockIntent = mock(PaymentIntent.class);
      EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

      when(mockIntent.getId()).thenReturn("pi_test_123");
      when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
      when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
      when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));

      webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
          .thenReturn(mockEvent);

      User user = mockUser();
      Payment payment = new Payment();
      payment.setId(42L);
      payment.setPaymentIntentId("pi_test_123");
      payment.setPurchasedPackage(CreditsPurchasedAmount.BUY_50_CREDITS);
      payment.setTransactionInitiator(user);
      payment.setStatus(PaymentStatus.SUCCEEDED);
      payment.setCreditsAwarded(true);

      when(paymentRepository.findByPaymentIntentId("pi_test_123"))
          .thenReturn(Optional.of(payment));

      paymentService.processWebhook("bomboo", "claat");

      assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());

      verify(paymentRepository, never()).save(any(Payment.class));

      verify(creditService, never()).addPurchasedCredits(anyLong(), anyInt());
    }
  }

  // 3. processWebhook_succeeded_creditsAlreadyAwarded_doesNothing()
  @Test
  void processWebhook_succeeded_creditsAlreadyAwarded_doesNothing() {
    try (MockedStatic<Webhook> mockWebhook = mockStatic(Webhook.class)) {

      Event mockedEvent = mock(Event.class);
      PaymentIntent mockedIntent = mock(PaymentIntent.class);
      EventDataObjectDeserializer mockedDeserializer = mock(EventDataObjectDeserializer.class);

      when(mockedIntent.getId()).thenReturn("pi_test_123");
      when(mockedEvent.getType()).thenReturn("payment_intent.succeeded");
      when(mockedEvent.getDataObjectDeserializer()).thenReturn(mockedDeserializer);
      when(mockedDeserializer.getObject()).thenReturn(Optional.of(mockedIntent));

      mockWebhook.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
          .thenReturn(mockedEvent);

      User user = mockUser();
      Payment payment = new Payment();
      payment.setId(42L);
      payment.setPaymentIntentId("pi_test_123");
      payment.setPurchasedPackage(CreditsPurchasedAmount.BUY_50_CREDITS);
      payment.setTransactionInitiator(user);
      payment.setStatus(PaymentStatus.PENDING);
      payment.setCreditsAwarded(true);

      when(paymentRepository.findByPaymentIntentId("pi_test_123"))
          .thenReturn(Optional.of(payment));

      paymentService.processWebhook("bomboo", "claat");

      verify(paymentRepository, never()).save(any(Payment.class));

      verify(creditService, never()).addPurchasedCredits(anyLong(), anyInt());
    }
  }

  // 4. processWebhook_succeeded_creditServiceThrows_doesNotCommitChanges()
  @Test
  void processWebhook_succeeded_creditServiceThrows_doesNotCommitChanges() {
    try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {

      Event mockEvent = mock(Event.class);
      PaymentIntent mockIntent = mock(PaymentIntent.class);
      EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

      when(mockIntent.getId()).thenReturn("pi_test_123");
      when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
      when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
      when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));

      webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
          .thenReturn(mockEvent);

      User user = mockUser();
      Payment payment = new Payment();
      payment.setId(42L);
      payment.setPaymentIntentId("pi_test_123");
      payment.setPurchasedPackage(CreditsPurchasedAmount.BUY_50_CREDITS);
      payment.setTransactionInitiator(user);
      payment.setStatus(PaymentStatus.PENDING);
      payment.setCreditsAwarded(false);

      when(paymentRepository.findByPaymentIntentId("pi_test_123"))
          .thenReturn(Optional.of(payment));

      // Simulate failure in credit awarding
      doThrow(new RuntimeException("Credit service unavailable"))
          .when(creditService).addPurchasedCredits(user.getId(), 50);

      assertThrows(RuntimeException.class,
          () -> paymentService.processWebhook("fake-payload", "fake-signature"));

      // Assert no commit happened (in unit test we see the state before throw)
      assertEquals(PaymentStatus.PENDING, payment.getStatus());
      assertFalse(payment.getCreditsAwarded());

      verify(paymentRepository, never()).save(any(Payment.class));
      // creditService called but threw
      verify(creditService).addPurchasedCredits(user.getId(), 50);
    }
  }

  // 5. processWebhook_failed_marksAsFailed()
  @Test
  void processWebhook_failed_marksAsFailed() {
    try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {

      Event mockEvent = mock(Event.class);
      PaymentIntent mockIntent = mock(PaymentIntent.class);
      EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

      when(mockIntent.getId()).thenReturn("pi_fail_456");
      when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
      when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
      when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));

      webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
          .thenReturn(mockEvent);

      User user = mockUser();
      Payment payment = new Payment();
      payment.setId(99L);
      payment.setPaymentIntentId("pi_fail_456");
      payment.setPurchasedPackage(CreditsPurchasedAmount.BUY_25_CREDITS);
      payment.setTransactionInitiator(user);
      payment.setStatus(PaymentStatus.PENDING);
      payment.setCreditsAwarded(false);

      when(paymentRepository.findByPaymentIntentId("pi_fail_456"))
          .thenReturn(Optional.of(payment));

      paymentService.processWebhook("fake-payload", "fake-signature");

      assertEquals(PaymentStatus.FAILED, payment.getStatus());
      verify(paymentRepository).save(payment);
      verify(creditService, never()).addPurchasedCredits(anyLong(), anyInt());
    }
  }

  // 6. processWebhook_failed_alreadyFailed_doesNothing()
  @Test
  void processWebhook_failed_alreadyFailed_doesNothing() {
    try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {

      Event mockEvent = mock(Event.class);
      PaymentIntent mockIntent = mock(PaymentIntent.class);
      EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

      when(mockIntent.getId()).thenReturn("pi_fail_456");
      when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
      when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
      when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));

      webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
          .thenReturn(mockEvent);

      Payment payment = new Payment();
      payment.setId(99L);
      payment.setPaymentIntentId("pi_fail_456");
      payment.setStatus(PaymentStatus.FAILED);

      when(paymentRepository.findByPaymentIntentId("pi_fail_456"))
          .thenReturn(Optional.of(payment));

      paymentService.processWebhook("fake-payload", "fake-signature");

      // No change, no save
      assertEquals(PaymentStatus.FAILED, payment.getStatus());
      verify(paymentRepository, never()).save(any(Payment.class));
      verify(creditService, never()).addPurchasedCredits(anyLong(), anyInt());
    }
  }

  // 7. processWebhook_unknownEventType_noSideEffects()
  @Test
  void processWebhook_unknownEventType_noSideEffects() {
    try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {

      Event mockEvent = mock(Event.class);
      when(mockEvent.getType()).thenReturn("charge.refunded"); // unhandled

      webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
          .thenReturn(mockEvent);

      paymentService.processWebhook("fake-payload", "fake-signature");

      // No DB or credit interactions
      verifyNoInteractions(paymentRepository);
      verifyNoInteractions(creditService);
    }
  }

  // 8. processWebhook_success_paymentNotFound_throwsNotFound()
  @Test
  void processWebhook_success_paymentNotFound_throwsNotFound() {
    try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {

      Event mockEvent = mock(Event.class);
      PaymentIntent mockIntent = mock(PaymentIntent.class);
      EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

      when(mockIntent.getId()).thenReturn("pi_missing_789");
      when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
      when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
      when(mockDeserializer.getObject()).thenReturn(Optional.of(mockIntent));

      webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
          .thenReturn(mockEvent);

      when(paymentRepository.findByPaymentIntentId("pi_missing_789"))
          .thenReturn(Optional.empty());

      WebhookProcessingException ex = assertThrows(WebhookProcessingException.class,
          () -> paymentService.processWebhook("fake-payload", "fake-signature"));

      assertTrue(ex.getCause() instanceof ResponseStatusException);
      ResponseStatusException cause = (ResponseStatusException) ex.getCause();
      assertEquals(HttpStatus.NOT_FOUND, cause.getStatusCode());
      assertEquals("Payment not found", cause.getReason());

      // No further side effects
      verify(creditService, never()).addPurchasedCredits(anyLong(), anyInt());
      verify(paymentRepository, never()).save(any(Payment.class));
    }
  }

}
