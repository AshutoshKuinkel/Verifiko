package com.verifico.server.auth.reset;

import java.time.LocalDateTime;

import com.verifico.server.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reset_tokens", indexes = {
    @Index(name = "idx_reset_token_value", columnList = "token"),
    @Index(name = "idx_reset_token_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ResetToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  public boolean isExpired() {
    return expiresAt.isBefore(LocalDateTime.now());
  }
}
