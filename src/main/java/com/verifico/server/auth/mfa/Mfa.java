package com.verifico.server.auth.mfa;

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
@Table(name = "mfa", indexes = {
    @Index(name = "mfa_token", columnList = "mfaCode"),
    @Index(name = "mfa_token_usr_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Mfa {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String mfaCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private boolean used = false;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false)
  private int attempts = 0;

  public boolean isMfaTokenExpired() {
    return expiresAt.isBefore(LocalDateTime.now());
  }

}
