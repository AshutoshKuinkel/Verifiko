package com.verifico.server.auth;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.auth.dto.RegisterRequest;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;
import com.verifico.server.user.dto.UserResponse;

// unit tests for auth endpoints(happy path,duplicates password hashing):
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

  @Mock
  UserRepository userRepository;

  @Mock
  PasswordEncoder passwordEncoder;

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

    when(userRepository.findByUsername(registerRequest.getEmail())).thenReturn(Optional.empty());
    when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(new User()));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, ()-> authService.register(registerRequest));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals("Email already in use", exception.getReason());

    verify(userRepository,never()).save(any());

  }

  @Test
  void duplicateUsername() {
    RegisterRequest registerRequest = validRegisterRequest();

    when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.of(new User()));

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, ()-> authService.register(registerRequest));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals("Username already in use", exception.getReason());
  }

  @Test
  void checkForPassHashing() {
  }
}
