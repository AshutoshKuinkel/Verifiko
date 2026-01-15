package com.verifico.server.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.post.dto.PostResponse;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;
import com.verifico.server.user.dto.AuthorResponse;
import com.verifico.server.post.dto.PostRequest;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
  private final UserRepository userRepository;
  private final PostRepository postRepository;

  @Transactional
  public PostResponse createPost(PostRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();

    User author = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to get username"));

    Post post = new Post();
    post.setAuthor(author);
    post.setTitle(request.getTitle());
    post.setTagline(request.getTagline());
    post.setCategory(request.getCategory());
    post.setStage(request.getStage());
    post.setProblemDescription(request.getProblemDescription());
    post.setSolutionDescription(request.getSolutionDescription());

    if (request.getScreenshotUrls() != null) {
      post.setScreenshotUrls(request.getScreenshotUrls());
    }
    if (request.getLiveDemoUrl() != null) {
      post.setLiveDemoUrl(request.getLiveDemoUrl());
    }

    Post savedPost = postRepository.save(post);

    // I need to have a field called totalPosts for user profile info, and then
    // increment it here whenever user makes a post
    userRepository.save(author);

    return toPostResponse(savedPost);
  }

  public PostResponse getPostById(Long id) {
    Post post = postRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

    return toPostResponse(post);

  }

  public Page<PostResponse> getAllPosts(int page, int size, Category category) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Post> posts;
    
    if (category != null){
      posts = postRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    } else{
      posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    return posts.map(this::toPostResponse);

  }

  private PostResponse toPostResponse(Post post) {
    return new PostResponse(
        post.getId(),
        toAuthorResponse(post.getAuthor()),
        post.getTitle(),
        post.getTagline(),
        post.getCategory(),
        post.getStage(),
        post.getProblemDescription(),
        post.getSolutionDescription(),
        post.getScreenshotUrls(),
        post.getLiveDemoUrl(),
        post.isBoosted(),
        post.getBoostedUntil(),
        post.getCreatedAt(),
        post.getUpdatedAt());
  }

  private AuthorResponse toAuthorResponse(User user) {
    return new AuthorResponse(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(),
        user.getAvatarUrl());
  }
}
