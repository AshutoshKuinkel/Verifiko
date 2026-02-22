package com.verifico.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResetRequest {
  @NotBlank(message = "Email required")
  @Email(message = "Email must be valid")
  private String email;
}