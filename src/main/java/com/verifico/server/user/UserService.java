package com.verifico.server.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.auth.token.RefreshTokenRepository;
import com.verifico.server.email.BrevoEmailService;
import com.verifico.server.user.dto.ProfileRequest;
import com.verifico.server.user.dto.PublicUserResponse;
import com.verifico.server.user.dto.UpdatePasswordRequest;
import com.verifico.server.user.dto.UserResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  private final PasswordEncoder passwordEncoder;

  private final RefreshTokenRepository refreshTokenRepository;

  private final BrevoEmailService emailService;

  public UserResponse meEndpoint() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to find authenticated user");
    }
    String username = auth.getName();

    User user = userRepository.findByUsername(username).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
            "Unable to find user associated with that username."));

    return toUserResponse(user);
  }

  public PublicUserResponse viewSomebodiesProfile(Long id) {
    // no need authentication or anything to view some body elses profile
    // just find by id and return the public user response..

    User user = userRepository.findById(id).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "A user with that associated id couldn't be found"));

    return toPublicUserResponse(user);
  }

  @Transactional
  public UserResponse updateMyProfile(ProfileRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found!");
    }
    String username = auth.getName();

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "User couldn't be found"));

    // update fields if entered
    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail()) && !request.getEmail().isBlank()) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
      }
      user.setEmail(request.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
    }
    if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())
        && !request.getUsername().isBlank()) {
      if (userRepository.existsByUsername(request.getUsername())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
      }
      user.setUsername(request.getUsername().trim());
    }
    if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
      user.setFirstName(request.getFirstName().trim());
    }
    if (request.getLastName() != null && !request.getLastName().isBlank()) {
      user.setLastName(request.getLastName().trim());
    }
    if (request.getBio() != null) {
      user.setBio(request.getBio().isBlank() ? null : request.getBio().trim());
    }
    if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
      String url = request.getAvatarUrl().trim();

      if (!url.startsWith("https://")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar URL must be HTTPS");
      }

      if (url.length() > 2048) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar URL too long");
      }
      user.setAvatarUrl(url);
    }

    User updatedUser = userRepository.save(user);
    return toUserResponse(updatedUser);

  }

  @Transactional
  public void updatePassword(UpdatePasswordRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found!");
    }

    String username = authentication.getName();

    User user = userRepository.findByUsername(username).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "A user with that associated id couldn't be found"));

    // check old pass matches user password
    if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect Current Password");
    }

    // check new pass doesn't equal the old pass
    if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be the same as old password");
    }

    // check that the new pass and confirm pass match
    if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Confirm password and New password fields do not match");
    }

    // hash new pass
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));

    // save
    userRepository.save(user);

    // invalidating all refresh tokens so user has to login again
    refreshTokenRepository.deleteByUserId(user.getId());

    emailService.sendPasswordChangedEmailForv1(user);
  }

  public UserResponse toUserResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getBio(),
        user.getAvatarUrl(),
        user.getJoinedDate());
  }

  public PublicUserResponse toPublicUserResponse(User user) {
    return new PublicUserResponse(
        user.getUsername(),
        user.getFirstName(),
        user.getLastName(),
        user.getBio(),
        user.getAvatarUrl(),
        user.getJoinedDate());
  }
}
