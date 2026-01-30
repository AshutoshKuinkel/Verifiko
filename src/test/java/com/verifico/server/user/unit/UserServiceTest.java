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
import com.verifico.server.user.dto.PublicUserResponse;
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
    user.setFirstName("John");
    user.setLastName("Doe");
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

  // vieweing other peoples profile endpoint tests:
  // id not found
  // successfully fecthed
  @Test
  void idNotFoundWhenSearchingForSomeoneProfile() {
    when(userRepository.findById(4L)).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> userService.viewSomebodiesProfile(4L));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("A user with that associated id couldn't be found", ex.getReason());
  }

  @Test
  void successfullyFetchingSomebodyProfile() {
    User user = mockUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    PublicUserResponse response = userService.viewSomebodiesProfile(1L);

    assertEquals(user.getUsername(), response.username());
    assertEquals(user.getFirstName(), response.firstName());
    assertEquals(user.getLastName(), response.LastName());
  }

  // update profile endpoint tests:
  // 1. unauthenticated user

  // 2. user not found in db

  // 3. email already in use

  // 4. username already in use

  // 5. avatar url not starting with https://
  // long avatar url

  // 6. successfull partial update

  // 7. given email is same as current email (make sure this don't hit db)

  // 8. given username is the same as current username (make sure this don't hit db)

  // 9. Blank email→ no update to email, no uniqueness check (make sure this don't
  // hit db)

  // 10. blank username, no update to username, no uniqueness check (make sure this
  // don't hit db)

  // 11. First name / last name blank → no update (make sure this
  // don't hit db)

  // 12. Bio provided as blank or only whitespace → bio is set to ""

  // 13. Bio provided with value (including leading/trailing spaces) → bio is trimmed
  // and updated

  // 14. Multiple fields updated successfully in one request (e.g. firstName + bio +
  // avatarUrl)

  // 15. No fields provided at all (all null or blank) → no changes, but save is still
  // called (no-op update)

  // 16. Email provided with leading/trailing spaces → email is trimmed and lowercased
  // correctly

  // 17. Username provided with leading/trailing spaces → username is trimmed but case
  // preserved
}
