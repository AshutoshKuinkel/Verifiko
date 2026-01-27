package com.verifico.server.post.dto;

import java.util.List;

import com.verifico.server.post.Category;
import com.verifico.server.post.Stage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostRequest {
  
  @NotBlank(message = "Title required for post")
  @Size(min=3, max = 100, message = "Title must be between 3-100 characters")
  private String title;

  @NotBlank(message = "Tagline required for post")
  @Size(min=3, max = 150, message = "Tagline must be between 3-150 characters")
  private String tagline;

  @NotNull(message = "Category required for post")
  private Category category;

  @NotNull(message = "State project is in required")
  private Stage stage;

  @NotBlank(message = "Problem description required")
  @Size(max = 2500, message = "Problem description must not exceed 2500 characters")
  private String problemDescription; 

  @NotBlank(message = "Solution description required")
  @Size(max = 2500, message = "Solution description must not exceed 2500 characters")
  private String solutionDescription;

  @Size(max = 5, message = "Maximum 5 screenshots allowed")
  private List<String> screenshotUrls;

  @Size(max = 500, message = "Demo URL too long")
  private String liveDemoUrl;

}