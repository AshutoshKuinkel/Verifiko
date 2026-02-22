package com.verifico.server.auth.reset;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {
  Optional<ResetToken> findByToken(String token);

  // used in cleanup scheduler
  void deleteAllByExpiresAtBefore(LocalDateTime now);

  void deleteByUserId(Long userId);
}
