package com.verifico.server.user;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verifico.server.common.dto.APIResponse;
import com.verifico.server.user.dto.ProfileRequest;
import com.verifico.server.user.dto.PublicUserResponse;
import com.verifico.server.user.dto.UserResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequestMapping("/api/users")
@RequiredArgsConstructor
// view someone elses profile by id, /me, search for people,
// update my profile:
// GET /api/users/me
// PUT /api/users/me
// DELETE /api/users/me (maybe soft delete, if this is even included)
// GET /api/users/userId
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public ResponseEntity<APIResponse<UserResponse>> profile() {
    UserResponse response = userService.meEndpoint();

    return ResponseEntity.ok()
        .body(new APIResponse<>("Successfully Fetched Profile", response));
  }

  @GetMapping("/{id}")
  public ResponseEntity<APIResponse<PublicUserResponse>> getUserProfile(@PathVariable @Positive Long id) {
    PublicUserResponse response = userService.viewSomebodiesProfile(id);

    return ResponseEntity.ok()
        .body(new APIResponse<>("Successfully Fetched User Profile", response));
  }

  @PatchMapping("/me")
  public ResponseEntity<APIResponse<UserResponse>> updateMyProfile(@Valid @RequestBody ProfileRequest request) {
    UserResponse updatedUser = userService.updateMyProfile(request);

    return ResponseEntity.ok()
        .body(new APIResponse<>("Profile successfully updated", updatedUser));
  }

}
