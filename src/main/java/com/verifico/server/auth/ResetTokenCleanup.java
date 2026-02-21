package com.verifico.server.auth;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.AllArgsConstructor;

// If some deeekhead user gets reset link but doesn't actually do anyting with it,
// abandonds it, or just doesn't even use it before the expiry then we get a 
// lotta token pile up... could pollute db later on down the line so it's better
// if we have this in comibnation with the delete token when user successfully changes
// password, we can ensure we don't get reset tokens piled up in our db.
@Configuration
@EnableScheduling
@AllArgsConstructor
public class ResetTokenCleanup {

  private final ResetTokenRepository resetTokenRepository;

  @Scheduled(fixedRate = 3_600_000)
  public void clearExpiredResetTokens() {
    resetTokenRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
  }
}
