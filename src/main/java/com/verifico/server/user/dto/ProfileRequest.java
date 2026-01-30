package com.verifico.server.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileRequest {
  @Size(min = 3, max = 16, message = "Username must be 3–16 characters")
  private String username;

  @Size(min = 3, max = 30, message = "First Name must be 3-30 characters")
  private String firstName;

  @Size(min = 3, max = 30, message = "Last Name must be 3-30 characters")
  private String lastName;

  @Email(message = "Email must be valid")
  private String email;

  @Size(max = 500, message = "Bio must not exceed 500 characters")
  private String bio;

  private String avatarUrl;
}
