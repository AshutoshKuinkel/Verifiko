package com.verifico.server.auth.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.auth.AuthService;
import com.verifico.server.auth.dto.LoginRequest;
import com.verifico.server.auth.dto.RegisterRequest;
import com.verifico.server.auth.jwt.JWTService;
import com.verifico.server.auth.mfa.MfaService;
import com.verifico.server.auth.token.RefreshToken;
import com.verifico.server.auth.token.RefreshTokenService;
import com.verifico.server.email.EmailService;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;
import com.verifico.server.user.dto.UserResponse;

// unit tests for auth endpoints(happy path,duplicates password hashing):
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  UserRepository userRepository;
  @Mock
  BCryptPasswordEncoder passwordEncoder;
  @Mock
  JWTService jwtService;
  @Mock
  RefreshTokenService refreshTokenService;
  @Mock
  EmailService emailService;
  @Mock
  MfaService mfaService;

  @InjectMocks
  AuthService authService;

  private RegisterRequest validRegisterRequest() {
    RegisterRequest registerRequest = new RegisterRequest();
    registerRequest.setUsername("JohnDoe123");
    registerRequest.setFirstName("John");
    registerRequest.setLastName("Doe");
    registerRequest.setEmail("johndoe2@gmail.com");
    registerRequest.setPassword("password123");
    registerRequest.setBio("Hey! My name is John and I am building a cool MVP, details on it in my posting.");

    return registerRequest;
  }

  @Test
  void registerHappyPath() {
    RegisterRequest registerRequest = validRegisterRequest();

    when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.empty());
    when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPass");

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setUsername(registerRequest.getUsername());
    savedUser.setFirstName(registerRequest.getFirstName());
    savedUser.setLastName(registerRequest.getLastName());
    savedUser.setEmail(registerRequest.getEmail());
    savedUser.setPassword(registerRequest.getPassword());
    savedUser.setBio(registerRequest.getBio());
    savedUser.setAvatarUrl(registerRequest.getAvatarUrl());

    when(userRepository.save(any(User.class))).thenReturn(savedUser); // stubbing, When authService calls
                                                                      // userRepository.save() with any User, pretend
                                                                      // the DB saved it and return this savedUser.

    UserResponse response = authService.register(registerRequest);

    assertNotNull(response);
    assertEquals("JohnDoe123", response.username());
    assertEquals("johndoe2@gmail.com", response.email());

    verify(userRepository).save(any(User.class)); // verification line, we're asserting “Yes, the service actually
                                                  // attempted to persist the user.” If this line isn't called then
                                                  // save() probably wasn't called and we have validation failed,
                                                  // exception thrown, logic path not reaching persistence...

  }

  @Test
  void duplicateEmail() {
    RegisterRequest registerRequest = validRegisterRequest();

    when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.empty());
    when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(new User()));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> authService.register(registerRequest));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals("Email already in use", exception.getReason());

    verify(userRepository, never()).save(any());

  }

  @Test
  void duplicateUsername() {
    RegisterRequest registerRequest = validRegisterRequest();

    when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.of(new User()));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> authService.register(registerRequest));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals("Username already in use", exception.getReason());
  }

  @Test
  void checkForPassHashing() {
    RegisterRequest registerRequest = validRegisterRequest();

    when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.empty());
    when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPass");

    User savedUser = new User();
    savedUser.setId(1L);
    savedUser.setUsername(registerRequest.getUsername());
    savedUser.setFirstName(registerRequest.getFirstName());
    savedUser.setLastName(registerRequest.getLastName());
    savedUser.setEmail(registerRequest.getEmail());
    savedUser.setPassword("hashedPass");
    savedUser.setBio(registerRequest.getBio());
    savedUser.setAvatarUrl(registerRequest.getAvatarUrl());

    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    authService.register(registerRequest);

    verify(passwordEncoder).encode("password123");
  }

  // tests for login endpoint: (check missing input, happy path, check invalid
  // credentials)
  // to be more specific:
  // 1. Neither username nor email provided
  // 2. Both username AND email provided
  // 3. User not found
  // 4. Password mismatch
  // 5. Valid username + password
  // 6. Valid email + password

  private LoginRequest loginWithUsername(String username, String password) {
    LoginRequest req = new LoginRequest();
    req.setUsername(username);
    req.setPassword(password);
    return req;
  }

  private LoginRequest loginWithEmail(String email, String password) {
    LoginRequest req = new LoginRequest();
    req.setEmail(email);
    req.setPassword(password);
    return req;
  }

  private User mockUser() {
    User user = new User();
    user.setId(1L);
    user.setUsername("JohnDoe123");
    user.setEmail("johndoe2@gmail.com");
    user.setPassword("hashedPass");
    return user;
  }

  @Test
  void loginFailsWhenUsernameAndEmailMissing() {
    LoginRequest request = new LoginRequest();
    request.setPassword("password123");

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Username or email is required", ex.getReason());
  }

  @Test
  void loginFailsWhenBothUsernameAndEmailProvided() {
    LoginRequest req = new LoginRequest();
    req.setUsername("JohnDoe123");
    req.setEmail("johndoe2@gmail.com");
    req.setPassword("password123");

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(req));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Provide either username or email, not both", ex.getReason());
  }

  @Test
  void userNotFoundOnLogin() {
    LoginRequest req = loginWithUsername("GIGGANIGGA", "password123");

    when(userRepository.findByUsername("GIGGANIGGA")).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(req));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertEquals("Invalid credentials", ex.getReason());
  }

  @Test
  void invalidPasswordOnLogin() {
    User user = mockUser();
    LoginRequest req = loginWithUsername("JohnDoe123", "wronggggpassword");

    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wronggggpassword", "hashedPass")).thenReturn(false);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(req));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertEquals("Invalid credentials", ex.getReason());
  }

  @Test
  void successfullLoginWithUsername() {
    User user = mockUser();
    LoginRequest req = loginWithUsername("JohnDoe123", "password123");

    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashedPass")).thenReturn(true);
    when(jwtService.generateAccessToken(1L, "JohnDoe123")).thenReturn("access-token");

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken("refresh-token");
    refreshToken.setUser(user);

    when(refreshTokenService.createToken(user)).thenReturn(refreshToken);

    var response = authService.login(req);
    assertNotNull(response);
    assertEquals("JohnDoe123", response.getUsername());
    assertEquals("access-token", response.getAccessToken());
    assertEquals("refresh-token", response.getRefreshToken());

    verify(passwordEncoder).matches("password123", "hashedPass");
    verify(jwtService).generateAccessToken(1L, "JohnDoe123");
    verify(refreshTokenService).createToken(user);
  }

  @Test
  void successfullLoginWithEmail() {
    User user = mockUser();
    LoginRequest req = loginWithEmail("johndoe2@gmail.com", "password123");

    when(userRepository.findByEmail("johndoe2@gmail.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashedPass")).thenReturn(true);
    when(jwtService.generateAccessToken(1L, "JohnDoe123")).thenReturn("access-token");

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken("refresh-token");
    refreshToken.setUser(user);

    when(refreshTokenService.createToken(user)).thenReturn(refreshToken);

    var response = authService.login(req);
    assertNotNull(response);
    assertEquals("JohnDoe123", response.getUsername());
    assertEquals("access-token", response.getAccessToken());
    assertEquals("refresh-token", response.getRefreshToken());

    verify(passwordEncoder).matches("password123", "hashedPass");
    verify(jwtService).generateAccessToken(1L, "JohnDoe123");
    verify(refreshTokenService).createToken(user);
  }
}
