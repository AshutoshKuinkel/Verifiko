package com.verifico.server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthorResponse {
  private Long id;
  private String username;
  private String firstName;
  private String lastName;
  private String avatarUrl;
}