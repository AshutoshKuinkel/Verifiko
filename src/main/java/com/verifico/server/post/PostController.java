package com.verifico.server.post;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verifico.server.common.dto.APIResponse;
import com.verifico.server.post.dto.PostRequest;
import com.verifico.server.post.dto.PostResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/post")
public class PostController {
  private final PostService postService;

  public PostController(PostService postService) {
    this.postService = postService;
  }

  @PostMapping("/create")
  public ResponseEntity<APIResponse<PostResponse>> createPost(@Valid @RequestBody PostRequest request) {
    PostResponse post = postService.createPost(request);

    return ResponseEntity.status(HttpStatus.CREATED.value())
        .body(new APIResponse<>("Post sucessfully created!", post));
  }

  @GetMapping("/{id}")
  public ResponseEntity<APIResponse<PostResponse>> getPostById(@PathVariable("id") Long id) {
    PostResponse post = postService.getPostById(id);

    return ResponseEntity.status(HttpStatus.OK)
        .body(new APIResponse<>("Successfully fetched post", post));
  }

}
