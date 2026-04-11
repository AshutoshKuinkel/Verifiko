package com.verifico.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

  private String username;

  @Email(message = "Email must be valid")
  private String email;

  @Size(min = 8, message = "Password must be at least 8 characters")
  private String password;

  @Size(min = 6, max = 6, message = "Please ensure your verification code is 6 characters long.")
  private String mfaCode;
}