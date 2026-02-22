package com.verifico.server.auth.dto;

import com.verifico.server.auth.validation.ValidPassword;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ResetPasswordRequest {

  private String token;

  @NotBlank(message = "Password required")
  @ValidPassword
  private String newPassword;
}
