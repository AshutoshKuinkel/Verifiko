package com.verifico.server.auth.mfa;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;

@Configuration
@EnableScheduling
@AllArgsConstructor
public class MfaCodeCleanup {
  // cleanup codes every hour
  private final MfaRepository mfaRepository;

  @Transactional
  @Scheduled(cron = "0 * * * *")
  public void cleanupPastMfaTokens() {
    mfaRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
  }
}
