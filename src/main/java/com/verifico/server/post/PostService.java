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
import com.verifico.server.post.dao.PostSearchDao;
import com.verifico.server.post.dto.PostRequest;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final PostSearchDao postSearchDao;

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

  public Page<PostResponse> getAllPosts(int page, int size, Category category, String search) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Post> posts;

    if (category != null) {
      posts = postRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    } else if (search != null) {
      posts = postSearchDao.searchPosts(search, pageable);
    } else {
      posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    return posts.map(this::toPostResponse);

  }

  @Transactional
  public PostResponse updatePostServiceById(Long id, PostRequest postRequest) {
    // security check first,
    // check if the user who posted is the one trying to update the post
    // I think we have a common username ground, on the access token and the post,
    // so we can compare those and see if they match for the check... We also have
    // to check if cookie is present ofc...
    Post post = postRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();

    if (!username.equals(post.getAuthor().getUsername())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorised to make changes to this post");
    }

    // maybe here we can just take in a new request body for post, and then
    // just save that, but if the field isn't eneterd i.e null then we keep
    // our original values
    if (postRequest.getTitle() != null) {
      post.setTitle(postRequest.getTitle());
    }
    if (postRequest.getTagline() != null) {
      post.setTagline(postRequest.getTagline());
    }
    if (postRequest.getCategory() != null) {
      post.setCategory(postRequest.getCategory());
    }
    if (postRequest.getStage() != null) {
      post.setStage(postRequest.getStage());
    }
    if (postRequest.getProblemDescription() != null) {
      post.setProblemDescription(postRequest.getProblemDescription());
    }
    if (postRequest.getSolutionDescription() != null) {
      post.setSolutionDescription(postRequest.getSolutionDescription());
    }
    if (postRequest.getScreenshotUrls() != null) {
      post.setScreenshotUrls(postRequest.getScreenshotUrls());
    }
    if (postRequest.getLiveDemoUrl() != null) {
      post.setLiveDemoUrl(postRequest.getLiveDemoUrl());
    }

    Post updatedPost = postRepository.save(post);
    return toPostResponse(updatedPost);
  }

  @Transactional
  public void deletePostbyId(Long id) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();

    Post post = postRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

    if (!username.equals(post.getAuthor().getUsername())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorised to make changes to this post");
    }

    postRepository.deleteById(id);
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
