package com.verifico.server.auth;

import java.time.Instant;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.auth.JWT.JWTService;
import com.verifico.server.auth.dto.LoginRequest;
import com.verifico.server.auth.dto.RegisterRequest;
import com.verifico.server.auth.token.RefreshToken;
import com.verifico.server.auth.token.RefreshTokenService;
import com.verifico.server.auth.dto.LoginResponse;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;
import com.verifico.server.user.dto.UserResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JWTService jwtService;
  private final RefreshTokenService refreshTokenService;
  @Value("${JWT_EXPIRY}")
  private int accessTokenMins;
  @Value("${REFRESH_TOKEN_DAYS}")
  private long RefreshTokenDays;

  public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JWTService jwtService,
      RefreshTokenService refreshTokenService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
  }

  @Transactional
  public UserResponse register(RegisterRequest request) {
    String username = request.getUsername().strip();
    String email = request.getEmail().strip().toLowerCase();
    String firstName = request.getFirstName().strip();
    String lastName = request.getLastName().strip();

    if (userRepository.findByUsername(username).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
    }

    if (userRepository.findByEmail(email).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
    }

    User user = new User();
    user.setUsername(username);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmail(email);
    user.setBio(request.getBio() != null ? request.getBio() : "");
    user.setAvatarUrl(request.getAvatarUrl());
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    User savedUser;
    try {
      savedUser = userRepository.save(user);
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already in use");
    }

    return new UserResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getFirstName(),
        savedUser.getLastName(), savedUser.getBio(), savedUser.getAvatarUrl(), savedUser.getJoinedDate());

  }

  public LoginResponse login(LoginRequest request) {

    if ((request.getUsername() == null || request.getUsername().isBlank()) &&
        (request.getEmail() == null || request.getEmail().isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username or email is required");
    }

    if (StringUtils.hasText(request.getUsername()) && StringUtils.hasText(request.getEmail())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide either username or email, not both");
    }

    User user = null;

    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      String email = request.getEmail().trim().toLowerCase();
      user = userRepository.findByEmail(email)
          .orElse(null); // Don't throw yet, we want same error message invalid cred latr
    }

    if (request.getUsername() != null && !request.getUsername().isBlank()) {
      String username = request.getUsername().trim();
      user = userRepository.findByUsername(username).orElse(null);
    }

    if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());

    RefreshToken refreshToken = refreshTokenService.createToken(user);

    return new LoginResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        accessToken, // this cannot be sent as a json response to client in prod
        refreshToken.getToken()); // this cannot be sent as a json response to client in prod
  }

  public LoginResponse refresh(String refreshToken) {
    // check access token has expired is already done in our other auth logic, so we
    // just need to validate the refresh token (check it exists in db + isn't
    // expired or revoked) and then delete current
    // refresh token and then generate new access/refresh token..

    RefreshToken token = refreshTokenService.findByToken(refreshToken);

    if (token.isRevoked()) {
      refreshTokenService.revokeAllForUser(token.getUser());
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Refresh token reuse detected");
    }

    if (token.getExpiryDate().isBefore(Instant.now())) {
      refreshTokenService.revoke(token);
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Refresh token expired");
    }

    User user = token.getUser();

    // deleteing old token + creating new:
    refreshTokenService.revoke(token);
    RefreshToken newRefreshToken = refreshTokenService.createToken(user);

    // create new access token aswell:
    String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());

    return new LoginResponse(user.getId(), user.getUsername(), user.getEmail(), newAccessToken,
        newRefreshToken.getToken());

  }

  public void logout(HttpServletRequest request) {
    // clear refresh token + access token if user goes to logout endpoint {this is
    // handeled in controller logic}
    // also revoke all db refresh tokens for that user...

    if (request.getCookies() == null)
      return;

    String refreshToken = Arrays.stream(request.getCookies())
        .filter(c -> "refresh_token".equals(c.getName()))
        .findFirst()
        .map(Cookie::getValue)
        .orElse(null);

    if (refreshToken != null) {
      refreshTokenService.revokeByToken(refreshToken);
    }

  }
}
