package com.verifico.server.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verifico.server.auth.dto.LoginRequest;
import com.verifico.server.auth.dto.LoginResponse;
import com.verifico.server.auth.dto.RegisterRequest;
import com.verifico.server.user.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  @Value("${JWT_EXPIRY}")
  private int accessTokenMins;
  @Value("${REFRESH_TOKEN_DAYS}")
  private long RefreshTokenDays;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  };

  @PostMapping("/login")
  public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {

    LoginResponse response = authService.login(request);

    ResponseCookie accessCookie = ResponseCookie.from("access_token", response.getAccessToken())
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/")
        .maxAge(accessTokenMins * 60)
        .build();

    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", response.getRefreshToken())
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/auth/refresh")
        .maxAge(RefreshTokenDays * 24 * 60 * 60)
        .build();

        return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE,accessCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .build();
  }
}
