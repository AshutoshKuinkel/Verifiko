package com.verifico.server.user.dto;

import java.time.LocalDate;

public record PublicUserResponse(
    String username,
    String firstName,
    String LastName,
    String bio,
    String avatarUrl,
    LocalDate joinedDate) {
}
