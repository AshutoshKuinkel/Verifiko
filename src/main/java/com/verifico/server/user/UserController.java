package com.verifico.server.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verifico.server.common.dto.APIResponse;
import com.verifico.server.user.dto.UserResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
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
}
