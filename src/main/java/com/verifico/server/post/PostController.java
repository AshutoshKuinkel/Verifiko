package com.verifico.server.post;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verifico.server.common.dto.APIResponse;
import com.verifico.server.post.dto.PostRequest;
import com.verifico.server.post.dto.PostResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
  private final PostService postService;

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

  // I think I should be able to add my filtering and everything here by just
  // sending some req.queries..
  // What Im doing right now is implementing pagination so we only fetch like
  // 10-15 posts at max at a time...
  // then in the frontned we can use lazyloading in the frontend, so more only
  // loads when user scrolls down..
  @GetMapping("")
  public ResponseEntity<APIResponse<Page<PostResponse>>> getAllPosts(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "15") int size,
      @RequestParam(value = "category",required = false) String categoryName
    ) {

    if (page < 0) {
      page = 0;
    }

    if (size < 1 || size > 30) {
      size = 30;
    }

    Category category = null;
    if(categoryName != null && !categoryName.isEmpty()){
      category = Category.findByCategoryName(categoryName);
    }

    Page<PostResponse> posts = postService.getAllPosts(page, size,category);

    return ResponseEntity.ok()
        .body(new APIResponse<>("Successfully fetched all posts", posts));
  }

}
