package com.verifico.server.auth.token;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.user.User;

import jakarta.transaction.Transactional;

@Service
public class RefreshTokenService {

  @Value("${REFRESH_TOKEN_EXPIRY}")
  private long RefreshTokenDays;

  private final RefreshTokenRepository refreshTokenRepository;

  public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  @Transactional
  // create token
  public RefreshToken createToken(User user) {
    // deleting the current refresh token for that user if there is one:
    refreshTokenRepository.deleteByUserId(user.getId());
    refreshTokenRepository.flush();
        

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(user);
    refreshToken.setToken(UUID.randomUUID().toString());
    refreshToken.setExpiryDate(Instant.now().plus(RefreshTokenDays, ChronoUnit.DAYS));

    refreshToken.setRevoked(false);

    return refreshTokenRepository.save(refreshToken);
  }

  // validate token
  public RefreshToken validate(String token) {
    // if we find a token, and it's not expired then we can assume it has to be
    // valid...

    RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

    if (refreshToken.isRevoked()) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Refresh token reuse detected");
    }

    if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
    }

    return refreshToken;
  }

  @Transactional
  // revoke token
  public void revoke(RefreshToken refreshToken) {
    refreshToken.setRevoked(true);
    refreshTokenRepository.save(refreshToken);
  }

  @Transactional
  public RefreshToken findByToken(String token) {
    return refreshTokenRepository.findByToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
  }

  @Transactional
  public void revokeAllForUser(User user) {
    refreshTokenRepository.findByUser_Username(user.getUsername())
        .forEach(token -> {
          token.setRevoked(true);
          refreshTokenRepository.save(token);
        });
  }

  @Transactional
  public void revokeByToken(String request){
    refreshTokenRepository.findByToken(request)
    .ifPresent(token -> {
      token.setRevoked(true);
      refreshTokenRepository.save(token);
    });
  }
}
