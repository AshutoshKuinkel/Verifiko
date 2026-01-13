package com.verifico.server.post.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.verifico.server.post.Category;
import com.verifico.server.post.Stage;
import com.verifico.server.user.User;

public record PostResponse(
  Long id,
  User author,
  String title,
  String tagline,
  Category category,
  Stage stage,
  String problemDescription,
  String solutionDescription,
  List<String> screenshotUrls,
  String liveDemoUrl,
  boolean isBoosted,
  LocalDate boostedUntil,
  Instant createdAt,
  Instant updatedAt
) {
}
