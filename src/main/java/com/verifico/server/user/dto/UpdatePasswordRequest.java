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

  @Size(min = 6, max = 6, message = "Please ensure your verification code is 6 characters long.")
  private String mfaCode;

  @NotBlank(message = "Please enter old password")
  private String oldPassword;

  @NotBlank(message = "New Password Required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  private String newPassword;

  @NotBlank(message = "Confirm new pasword")
  private String confirmNewPassword;

}
