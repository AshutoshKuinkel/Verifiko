package com.verifico.server.credit.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.credit.CreditService;
import com.verifico.server.credit.CreditTransaction;
import com.verifico.server.credit.CreditTransactionRepository;
import com.verifico.server.credit.TransactionType;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {
  @Mock
  CreditTransactionRepository creditTransactionRepository;

  @Mock
  SecurityContext securityContext;

  @Mock
  UserRepository userRepository;

  @Mock
  Authentication authentication;

  @InjectMocks
  CreditService creditService;

  @BeforeEach
  void setup() {
    SecurityContextHolder.setContext(securityContext);
  }

  private User mockUser() {
    User user = new User();
    user.setId(1L);
    user.setUsername("JohnDoe123");
    user.setEmail("johndoe2@gmail.com");
    user.setPassword("hashedPass");
    return user;
  }

  // add credits endpoint:
  // user not found
  @Test
  void userNotFoundWhenAddingCredits() {
    when(userRepository.findById(4L)).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> creditService.addCredits(4L, null, null, null));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("User not found", ex.getReason());

    verify(creditTransactionRepository, never()).save(any());
  }

  // negative amount
  @Test
  void negativeAmountWhenAddingCredits() {
    User user = mockUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> creditService.addCredits(1L, TransactionType.CREATE_POST, null, null));

    assertEquals("addCredits cannot be used for negative transactions", ex.getMessage());

    verify(creditTransactionRepository, never()).save(any());
  }

  // PURCHASE_CREDITS type -> throws IllegalArgumentException
  @Test
  void purchaseCreditsTypeOnAddCredits() {
    User user = mockUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> creditService.addCredits(1L, TransactionType.PURCHASE_CREDITS, null, null));

    assertEquals("Use addPurchasedCredits() for purchases", ex.getMessage());

    verify(creditTransactionRepository, never()).save(any());
  }

  // successful add credits + verify balance updated
  @Test
  void successfullAddCreditsPlusVerifyBalanceUpdated() {
    User user = mockUser();
    user.setCredits(100);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenReturn(user);
    when(creditTransactionRepository.save(any(CreditTransaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreditTransaction response = creditService.addCredits(1L,
        TransactionType.COMMENT_MARKED_HELPFUL, null, null);

    assertNotNull(response);
    assertEquals(105, user.getCredits());
    assertEquals(105, response.getBalanceAfter());
    assertEquals(5, response.getAmount());
    assertEquals(TransactionType.COMMENT_MARKED_HELPFUL, response.getTransactionType());

    verify(userRepository).save(user);
    verify(creditTransactionRepository).save(any(CreditTransaction.class));
  }

  // spend credits endpoint:
  // user not found
  @Test
  void userNotFoundWhenSpendingCredits() {
    when(userRepository.findById(4L)).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> creditService.spendCredits(4L, null, null, null));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("User not found", ex.getReason());

    verify(creditTransactionRepository, never()).save(any());
  }

  // positive amount
  @Test
  void positiveAmountWhenSpendingCredits() {
    User user = mockUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> creditService.spendCredits(1L, TransactionType.COMMENT_MARKED_HELPFUL, null, null));

    assertEquals("spendCredits cannot be used for positive transactions", ex.getMessage());

    verify(creditTransactionRepository, never()).save(any());
  }

  // user doesn't have enough to spend
  @Test
  void userDoesntHaveEnoughCreditsToSpend() {
    User user = mockUser();
    user.setCredits(5);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> creditService.spendCredits(1L, TransactionType.CREATE_POST, null, null));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Insufficient credits. You have " + 5 + " but need " + 20
        + "Either buy more or contribute to the community", ex.getReason());

    verify(creditTransactionRepository, never()).save(any());
  }

  // PURCHASE_CREDITS type -> throws IllegalArgumentException (ADD)
  @Test
  void purchaseCreditsTypeOnSpendCredits() {
    User user = mockUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> creditService.spendCredits(1L, TransactionType.PURCHASE_CREDITS, null, null));

    assertEquals("Use addPurchasedCredits() for purchases", ex.getMessage());

    verify(creditTransactionRepository, never()).save(any());
  }

  // user has exactly enough to spend
  @Test
  void userHasExactlyEnoughToSpendCredits() {
    User user = mockUser();
    user.setCredits(20);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenReturn(user);
    when(creditTransactionRepository.save(any(CreditTransaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreditTransaction response = creditService.spendCredits(1L, TransactionType.CREATE_POST,
        null, null);

    assertNotNull(response);
    assertEquals(0, user.getCredits());
    assertEquals(0, response.getBalanceAfter());
    assertEquals(-20, response.getAmount());
    assertEquals(TransactionType.CREATE_POST, response.getTransactionType());

    verify(userRepository).save(user);
    verify(creditTransactionRepository).save(any(CreditTransaction.class));
  }

  // successful spend credits + verify balance updated
  @Test
  void userSuccessfullySpentCredits() {
    User user = mockUser();
    user.setCredits(100);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenReturn(user);
    when(creditTransactionRepository.save(any(CreditTransaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreditTransaction response = creditService.spendCredits(1L, TransactionType.CREATE_POST,
        null, null);

    assertNotNull(response);
    assertEquals(80, user.getCredits());
    assertEquals(80, response.getBalanceAfter());
    assertEquals(-20, response.getAmount());
    assertEquals(TransactionType.CREATE_POST, response.getTransactionType());

    verify(userRepository).save(user);
    verify(creditTransactionRepository).save(any(CreditTransaction.class));
  }

  // add purchased credits endpoint:
  // user not found
  // successful add purchased credits + verify balance updated (ENHANCE)

  // check balance endpoint:
  // null auth
  @Test
  void authIsNullOnCheckBalanceEndpoint() {
    when(securityContext.getAuthentication()).thenReturn(null);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> creditService.checkBalance());

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertEquals("Authenticated user not found!", ex.getReason());
  }

  // user not found by username
  @Test
  void userNotFoundByUserNameWhenCheckingBalance() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> creditService.checkBalance());

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("User not found", ex.getReason());
  }

  // successful balance check
  @Test
  void successfullBalanceCheck() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    User user = mockUser();

    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.of(user));

    int response = creditService.checkBalance();

    assertEquals(user.getCredits(), response);
  }

  // get my transactions endpoint:
  // null auth
  @Test
  void authIsNullOnGetMyTransactionsEndpoint() {
    when(securityContext.getAuthentication()).thenReturn(null);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> creditService.getTransactions(0, 0));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertEquals("Authenticated user not found!", ex.getReason());
  }

  // user not found by username
  @Test
  void userNotFoundByUserNameWhenGettingTransactionHistory() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> creditService.getTransactions(0, 0));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("User not found", ex.getReason());
  }

  // successful transactions fetch
  @Test
  void successfullyFetchedUserTransactions() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    User user = mockUser();
    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.of(user));

    Page<CreditTransaction> mockPage = Page.empty();
    when(creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(any(), any())).thenReturn(mockPage);

    Page<CreditTransaction> response = creditService.getTransactions(0, 15);

    assertNotNull(response);
    assertEquals(mockPage, response);

    verify(creditTransactionRepository).findByUserIdOrderByCreatedAtDesc(eq(1L), any());

  }
}
