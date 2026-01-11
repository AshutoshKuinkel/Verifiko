package com.verifico.server.post;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/post")
public class PostController {
  private final PostService postService;

  public PostController (PostService postService){
    this.postService = postService;
  }

  @PostMapping("/create")
  public void createPost(){
    return;
  }

}
