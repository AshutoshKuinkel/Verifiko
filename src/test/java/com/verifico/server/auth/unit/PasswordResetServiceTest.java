package com.verifico.server.auth.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.auth.dto.ResetPasswordRequest;
import com.verifico.server.auth.reset.PasswordResetService;
import com.verifico.server.auth.reset.ResetToken;
import com.verifico.server.auth.reset.ResetTokenRepository;
import com.verifico.server.email.EmailService;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock
  UserRepository userRepository;

  @Mock
  ResetTokenRepository resetTokenRepository;

  @Mock
  PasswordEncoder passwordEncoder;

  @Mock
  EmailService emailService;

  @InjectMocks
  PasswordResetService passwordResetService;

  private User testUser;
  private ResetToken validToken;
  private ResetToken expiredToken;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("test@verifiko.com");
    testUser.setUsername("testuser");
    testUser.setPassword("hashed_password");

    validToken = new ResetToken();
    validToken.setToken("valid-token-abc123");
    validToken.setUser(testUser);
    validToken.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // still valid

    expiredToken = new ResetToken();
    expiredToken.setToken("expired-token-xyz789");
    expiredToken.setUser(testUser);
    expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // already expired
  }

  // tests for passResetRequest endpoint:
  @Test
  void passResetRequest_unkoownEmail_returnsWithoutDoingAnything() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> passwordResetService.passResetRequest("bigbs@gmail.com"));

    verify(resetTokenRepository, never()).save(any());
    verify(emailService, never()).changePasswordResetLinkEmailForv1(anyString(), anyString());
  }

  @Test
  void passResetRequest_validEmail_deletesExistingTokenAndSavesNew() {
    when(userRepository.findByEmail("test@verifiko.com")).thenReturn(Optional.of(testUser));

    passwordResetService.passResetRequest("test@verifiko.com");

    verify(resetTokenRepository).deleteByUserId(testUser.getId());
    verify(resetTokenRepository).save(any(ResetToken.class));
  }

  @Test
  void passResetRequest_normalisesEmailBeforeLookup() {
    // Emails should be lowercased + stripped before hitting the repo
    when(userRepository.findByEmail("test@verifiko.com")).thenReturn(Optional.of(testUser));

    passwordResetService.passResetRequest("  TEST@VERIFIKO.COM  ");

    verify(userRepository).findByEmail("test@verifiko.com");
  }

  @Test
  void passResetRequest_validEmail_sendsResetEmail() {
    when(userRepository.findByEmail("test@verifiko.com")).thenReturn(Optional.of(testUser));

    passwordResetService.passResetRequest("test@verifiko.com");

    verify(emailService).changePasswordResetLinkEmailForv1(anyString(), eq("test@verifiko.com"));
  }

  // tests for validate token endpoint:
  @Test
  void validateToken_tokenNotFound_throwsBadRequest() {
    when(resetTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> passwordResetService.validateToken("nonexistent"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Invalid or expired token", ex.getReason());
  }

  @Test
  void validateToken_expiredToken_throwsBadRequest() {
    when(resetTokenRepository.findByToken("expired-token-xyz789")).thenReturn(Optional.of(expiredToken));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> passwordResetService.validateToken("expired-token-xyz789"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Invalid or expired token", ex.getReason());
  }

  @Test
  void validateToken_validToken_doesNotThrow() {
    when(resetTokenRepository.findByToken("valid-token-abc123")).thenReturn(Optional.of(validToken));

    assertDoesNotThrow(() -> passwordResetService.validateToken("valid-token-abc123"));
  }

  // tests for resetPass endpoint:
  @Test
  void resetPassword_invalidToken_throwsBadRequest() {
    when(resetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("bad-token");
    request.setNewPassword("NewPassword1!");

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> passwordResetService.resetPassword(request));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Token is invalid or expired", ex.getReason());

    verify(resetTokenRepository, never()).save(any());
    verify(emailService, never()).sendPasswordChangedEmailForv1(testUser);
  }

  @Test
  void resetPassword_expiredToken_throwsBadRequest() {
    when(resetTokenRepository.findByToken("expired-token-xyz789")).thenReturn(Optional.of(expiredToken));

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("expired-token-xyz789");
    request.setNewPassword("NewPassword1!");

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> passwordResetService.resetPassword(request));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Token is invalid or expired", ex.getReason());

    verify(resetTokenRepository, never()).save(any());
    verify(emailService, never()).sendPasswordChangedEmailForv1(testUser);
  }

  @Test
  void resetPassword_validToken_updatesPasswordAndDeletesToken() {
    when(resetTokenRepository.findByToken("valid-token-abc123")).thenReturn(Optional.of(validToken));
    when(passwordEncoder.encode("NewPassword1!")).thenReturn("hashed_new_password");

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("valid-token-abc123");
    request.setNewPassword("NewPassword1!");

    passwordResetService.resetPassword(request);

    verify(userRepository).save(testUser);
    verify(resetTokenRepository).delete(validToken); // one hit wonder, has to be deleted right after..
  }

  @Test
  void resetPassword_validToken_sendsPasswordChangedEmail() {
    when(resetTokenRepository.findByToken("valid-token-abc123")).thenReturn(Optional.of(validToken));
    when(passwordEncoder.encode(anyString())).thenReturn("hashed_new_password");

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("valid-token-abc123");
    request.setNewPassword("NewPassword1!");

    passwordResetService.resetPassword(request);

    verify(emailService).sendPasswordChangedEmailForv1(testUser);
  }

  // make reset token helper func, test:
  @Test
  void makeResetToken_returnsNonNullUniqueTokens() {
    String token1 = passwordResetService.makeResetToken();
    String token2 = passwordResetService.makeResetToken();

    assertNotNull(token1);
    assertNotNull(token2);

    assert (!token1.equals(token2));
  }
}
