package com.verifico.server.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
  private String message;
  private String username;
  private Long userId;

  public AuthResponse(String message){
    this.message = message;
  }
}
