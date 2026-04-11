package com.verifico.server.auth.mfa;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;

public interface MfaRepository extends JpaRepository<Mfa, Long> {
  Optional<Mfa> findTopByUser_IdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(Long userId, LocalDateTime now);

  @Transactional
  @Modifying
  @Query("UPDATE Mfa m SET m.used = true WHERE m.user.id = :userId AND m.used = false")
  void invalidateAllActiveForUser(@Param("userId") Long userId);

  // used in cleanup scheduler
  void deleteAllByExpiresAtBefore(LocalDateTime now);

}
