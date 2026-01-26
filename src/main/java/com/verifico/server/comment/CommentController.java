package com.verifico.server.comment;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verifico.server.comment.dto.CommentRequest;
import com.verifico.server.comment.dto.CommentResponse;
import com.verifico.server.common.dto.APIResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @PostMapping("/posts/{id}/comments")
  public ResponseEntity<APIResponse<CommentResponse>> createComment(@Valid @RequestBody CommentRequest request,
      @PathVariable("id") Long id) {
    CommentResponse response = commentService.postComment(request, id);

    return ResponseEntity.status(HttpStatus.CREATED.value())
        .body(new APIResponse<>("Comment Successfully Posted!", response));
  }

  @GetMapping("/posts/{id}/comments")
  public ResponseEntity<APIResponse<Page<CommentResponse>>> fetchAllCommentsForPost(@PathVariable("id") Long id,
      @RequestParam(value = "page", defaultValue = "0") int page) {

    if (page < 0 || page > 20) {
      page = 0;
    }

    int size = 15;

    Page<CommentResponse> response = commentService.getAllCommentsForPost(id, page, size);

    return ResponseEntity.ok()
        .body(new APIResponse<>("All comments fecthed!", response));

  }

  @DeleteMapping("/comments/{id}")
  public ResponseEntity<Void> deleteComment(@PathVariable("id") Long id) {
    commentService.deleteMyComment(id);

    return ResponseEntity.noContent().build();
  }
}
