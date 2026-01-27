package com.verifico.server.user.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;
import com.verifico.server.user.UserService;
import com.verifico.server.user.dto.UserResponse;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock
  UserRepository userRepository;

  @Mock
  SecurityContext securityContext;

  @Mock
  Authentication authentication;

  @InjectMocks
  UserService userService;

  private User mockUser() {
    User user = new User();
    user.setId(1L);
    user.setUsername("JohnDoe123");
    user.setEmail("johndoe2@gmail.com");
    user.setPassword("hashedPass");
    return user;
  }

  @BeforeEach
  void setup() {
    SecurityContextHolder.setContext(securityContext);
  }

  // me endpoint (check for unauthenticated user trying to get /me where auth =
  // null case, user not found in db case, success path)
  @Test
  void checkForUnauthenticatedUserAccessingProfileEndpoint() {
    when(securityContext.getAuthentication()).thenReturn(null);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.meEndpoint());

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertEquals("Unable to find authenticated user", ex.getReason());
  }

  @Test
  void checkForUserNotFoundInDBWhenFetchingProfile() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.meEndpoint());

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertEquals("Unable to find user associated with that username.", ex.getReason());

  }

  @Test
  void successfullyFetchMeProfile() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    User user = mockUser();

    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.of(user));

    UserResponse response = userService.meEndpoint();

    assertEquals(user.getId(), response.id());
    assertEquals(user.getUsername(), response.username());
    assertEquals(user.getEmail(), response.email());

  }
}
