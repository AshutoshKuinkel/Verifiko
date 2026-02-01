package com.verifico.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdatePasswordRequest {
  @NotBlank(message = "Please enter old password")
  private String oldPassword;

  @NotBlank(message = "New Password Required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  private String newPassword;

  @NotBlank(message = "Confirm new pasword")
  private String confirmNewPassword;

}
