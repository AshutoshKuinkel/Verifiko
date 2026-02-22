package com.verifico.server.auth.reset;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.auth.dto.ResetPasswordRequest;
import com.verifico.server.email.EmailService;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// overall flow outline:
// user clicks forgot pass in frontend
// user enters email... if email found
// generate unique token with like a 15-30 min expiry
// send reset link to user email with the token in link
// get user to answer 2-3 security questions they setup when
// creating their account
// then prompt them to change password
// reference article:
// https://medium.com/@AlexanderObregon/making-a-basic-password-reset-workflow-in-spring-boot-b845bb4f7cf9

@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private final UserRepository userRepository;
  private final ResetTokenRepository resetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;
  private static final SecureRandom secureRandom = new SecureRandom();
  private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

  public void passResetRequest(String email) {
    Optional<User> userOpt = userRepository.findByEmail(email.strip().toLowerCase());

    if (userOpt.isEmpty())
      return;

    User user = userOpt.get();

    // cleanup prior tokens, avoids case of having 2 available reset links for one
    // user in the case
    // that one expired yet and user gets another link:
    resetTokenRepository.deleteByUserId(user.getId());

    String token = makeResetToken();

    ResetToken resetToken = new ResetToken();
    resetToken.setToken(token);
    resetToken.setUser(user);
    // Let users know in the email the link will expire after 15 mins..
    resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));

    resetTokenRepository.save(resetToken);

    // send email to the given user with the token included... here:
    // key point: Nothing leaves server unless token is valid & tied to
    // an existing user:
    emailService.changePasswordResetLinkEmailForv1(token, email);
  }

  public void validateToken(String token) {
    Optional<ResetToken> tokenOpt = resetTokenRepository.findByToken(token);

    if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
    }
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    Optional<ResetToken> tokenOpt = resetTokenRepository.findByToken(request.getToken());

    if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is invalid or expired");
    }

    ResetToken fetchedToken = tokenOpt.get();
    User user = fetchedToken.getUser();

    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
    resetTokenRepository.delete(fetchedToken);

    // notifying user of password change:
    emailService.sendPasswordChangedEmailForv1(user);
  }

  // helpers:
  public String makeResetToken() {
    byte[] randomBytes = new byte[32]; // 256 bit token
    secureRandom.nextBytes(randomBytes);
    return base64Encoder.encodeToString(randomBytes);
  }

}
