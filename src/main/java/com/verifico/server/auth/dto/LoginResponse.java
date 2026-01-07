package com.verifico.server.auth.dto;


public class LoginResponse {

  private final Long userId;
  private final String username;
  private final String email;
  private final String accessToken;
  private final String refreshToken;

  public LoginResponse(
      Long userId,
      String username,
      String email,
      String accessToken,
      String refreshToken) {
    this.userId = userId;
    this.username = username;
    this.email = email;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }

  public Long getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }
}
