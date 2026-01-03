package com.verifico.server.auth;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.verifico.server.user.UserRepository;

// unit tests for auth endpoints(happy path,duplicates password hashing):
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

  @Mock
  UserRepository userRepository;

  @Mock
  PasswordEncoder passwordEncoder;

  @InjectMocks
  AuthService authService;

  @Test
  void registerHappyPath() {
  }

  @Test
  void duplicateEmail(){}

  @Test
  void duplicateUsername(){}

  @Test
  void checkForPassHashing(){}
}
