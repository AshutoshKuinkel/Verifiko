package com.verifico.server.auth.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.auth.mfa.Mfa;
import com.verifico.server.auth.mfa.MfaRepository;
import com.verifico.server.auth.mfa.MfaService;
import com.verifico.server.email.EmailService;
import com.verifico.server.user.User;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

  @Mock
  MfaRepository mfaRepository;

  @Mock
  EmailService emailService;

  @Mock
  BCryptPasswordEncoder passwordEncoder;

  @InjectMocks
  MfaService mfaService;

  private User mockUser() {
    User user = new User();
    user.setId(1L);
    user.setUsername("JohnDoe123");
    user.setEmail("johndoe2@gmail.com");
    return user;
  }

  private Mfa mockValidToken(String hashedCode) {
    Mfa token = new Mfa();
    token.setMfaCode(hashedCode);
    token.setUser(mockUser());
    token.setUsed(false);
    token.setAttempts(0);
    token.setExpiresAt(LocalDateTime.now().plusSeconds(600));
    return token;
  }

  // 1. Happy path – invalidates old tokens, saves new token, sends email
  @Test
  void happyPathGeneratesAndSavesTokenAndSendsEmail() {
    User user = mockUser();

    when(passwordEncoder.encode(any(String.class))).thenReturn("hashedCode");

    mfaService.mfaTokenGeneration(user);

    verify(mfaRepository).invalidateAllActiveForUser(user.getId());

    ArgumentCaptor<Mfa> captor = ArgumentCaptor.forClass(Mfa.class);
    verify(mfaRepository).save(captor.capture());

    Mfa saved = captor.getValue();
    assertNotNull(saved.getMfaCode());
    assertEquals(user, saved.getUser());
    assertFalse(saved.isUsed());
    assertEquals(0, saved.getAttempts());
    assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusSeconds(590)));
    assertTrue(saved.getExpiresAt().isBefore(LocalDateTime.now().plusSeconds(610)));

    // raw code goes to email, not the hash
    ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendMfaGmailCodeEmailForv1(codeCaptor.capture(), eq(user.getEmail()));
    assertEquals(6, codeCaptor.getValue().length());
  }

  // 2. Always invalidates before saving
  @Test
  void alwaysInvalidatesPreviousCodeBeforeGeneratingNew() {
    User user = mockUser();

    when(passwordEncoder.encode(any(String.class))).thenReturn("hashedCode");

    mfaService.mfaTokenGeneration(user);
    mfaService.mfaTokenGeneration(user);

    verify(mfaRepository, times(2)).invalidateAllActiveForUser(user.getId());
    verify(mfaRepository, times(2)).save(any(Mfa.class));
    verify(emailService, times(2)).sendMfaGmailCodeEmailForv1(any(), eq(user.getEmail()));
  }

  // 3. No active token found – throws BAD_REQUEST
  @Test
  void noActiveTokenFoundThrowsBadRequest() {
    User user = mockUser();

    when(mfaRepository.findTopByUser_IdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
        eq(user.getId()), any(LocalDateTime.class)))
        .thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> mfaService.validateMfaToken(user, "anyCode"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Invalid or expired code", ex.getReason());

    verify(mfaRepository, never()).save(any());
  }

  // 4. Token expired – throws BAD_REQUEST
  @Test
  void expiredTokenThrowsBadRequest() {
    User user = mockUser();

    Mfa expiredToken = mockValidToken("hashedCode");
    expiredToken.setExpiresAt(LocalDateTime.now().minusSeconds(1));

    when(mfaRepository.findTopByUser_IdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
        eq(user.getId()), any(LocalDateTime.class)))
        .thenReturn(Optional.of(expiredToken));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> mfaService.validateMfaToken(user, "anyCode"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Invalid or expired code", ex.getReason());

    verify(mfaRepository, never()).save(any());
  }

  // 5. Token already used – throws BAD_REQUEST
  @Test
  void alreadyUsedTokenThrowsBadRequest() {
    User user = mockUser();

    Mfa usedToken = mockValidToken("hashedCode");
    usedToken.setUsed(true);

    when(mfaRepository.findTopByUser_IdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
        eq(user.getId()), any(LocalDateTime.class)))
        .thenReturn(Optional.of(usedToken));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> mfaService.validateMfaToken(user, "anyCode"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Invalid or expired code", ex.getReason());

    verify(mfaRepository, never()).save(any());
  }

  // 6. Max attempts exceeded – throws TOO_MANY_REQUESTS
  @Test
  void maxAttemptsExceededThrowsTooManyRequests() {
    User user = mockUser();

    Mfa lockedToken = mockValidToken("hashedCode");
    lockedToken.setAttempts(5);

    when(mfaRepository.findTopByUser_IdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
        eq(user.getId()), any(LocalDateTime.class)))
        .thenReturn(Optional.of(lockedToken));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> mfaService.validateMfaToken(user, "anyCode"));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    assertEquals("Too many failed attempts, please request another code.", ex.getReason());

    verify(mfaRepository, never()).save(any());
  }

  // 7. Wrong code submitted – increments attempts, throws BAD_REQUEST
  @Test
  void wrongCodeIncrementsAttemptsAndThrowsBadRequest() {
    User user = mockUser();

    Mfa token = mockValidToken("hashedCode");
    token.setAttempts(2);

    when(mfaRepository.findTopByUser_IdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
        eq(user.getId()), any(LocalDateTime.class)))
        .thenReturn(Optional.of(token));

    when(passwordEncoder.matches("wrongCode", "hashedCode")).thenReturn(false);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> mfaService.validateMfaToken(user, "wrongCode"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Invalid code", ex.getReason());

    ArgumentCaptor<Mfa> captor = ArgumentCaptor.forClass(Mfa.class);
    verify(mfaRepository).save(captor.capture());

    Mfa saved = captor.getValue();
    assertEquals(3, saved.getAttempts());
    assertFalse(saved.isUsed());
  }

  // 8. Correct code – marks token as used, does not change attempts
  @Test
  void correctCodeMarksTokenAsUsedAndDoesNotChangeAttempts() {
    User user = mockUser();

    Mfa token = mockValidToken("hashedCode");
    token.setAttempts(2);

    when(mfaRepository.findTopByUser_IdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
        eq(user.getId()), any(LocalDateTime.class)))
        .thenReturn(Optional.of(token));

    when(passwordEncoder.matches("correctCode", "hashedCode")).thenReturn(true);

    assertDoesNotThrow(() -> mfaService.validateMfaToken(user, "correctCode"));

    ArgumentCaptor<Mfa> captor = ArgumentCaptor.forClass(Mfa.class);
    verify(mfaRepository).save(captor.capture());

    Mfa saved = captor.getValue();
    assertTrue(saved.isUsed());
    assertEquals(2, saved.getAttempts()); // unchanged
  }

  // 9. Edge – attempts at maxAttempts - 1, wrong code pushes to limit
  @Test
  void attemptsAtLimitMinusOneWrongCodePushesToMax() {
    User user = mockUser();

    Mfa token = mockValidToken("hashedCode");
    token.setAttempts(4); // one below max (5)

    when(mfaRepository.findTopByUser_IdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
        eq(user.getId()), any(LocalDateTime.class)))
        .thenReturn(Optional.of(token));

    when(passwordEncoder.matches("wrongCode", "hashedCode")).thenReturn(false);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> mfaService.validateMfaToken(user, "wrongCode"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Invalid code", ex.getReason());

    ArgumentCaptor<Mfa> captor = ArgumentCaptor.forClass(Mfa.class);
    verify(mfaRepository).save(captor.capture());

    assertEquals(5, captor.getValue().getAttempts()); // now at max
  }

}
