package com.verifico.server.auth.reset;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verifico.server.auth.dto.ResetPasswordRequest;
import com.verifico.server.auth.dto.ResetRequest;
import com.verifico.server.common.dto.APIResponse;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class PasswordResetController {

  private final PasswordResetService passwordResetService;

  @PostMapping("/reset-password")
  public ResponseEntity<APIResponse<String>> requestPassReset(@Valid @RequestBody ResetRequest request) {
    passwordResetService.passResetRequest(request.getEmail());
    return ResponseEntity.ok()
        .body(new APIResponse<>("If the email is associated with an account, you'll get a reset link", null));
  }

  @GetMapping("/reset-password")
  public ResponseEntity<APIResponse<String>> validateToken(@RequestParam("token") String token) {
    passwordResetService.validateToken(token);

    return ResponseEntity.ok()
        .body(new APIResponse<>("Valid Token", null));
  }

  @PostMapping("/reset-password/confirm")
  public ResponseEntity<APIResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request);

    return ResponseEntity.ok()
        .body(new APIResponse<>("Password Successfully Updated", null));
  }

}
