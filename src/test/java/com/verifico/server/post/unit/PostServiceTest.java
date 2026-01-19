package com.verifico.server.post.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.post.Category;
import com.verifico.server.post.Post;
import com.verifico.server.post.PostRepository;
import com.verifico.server.post.PostService;
import com.verifico.server.post.Stage;
import com.verifico.server.post.dao.PostSearchDao;
import com.verifico.server.post.dto.PostRequest;
import com.verifico.server.post.dto.PostResponse;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;

// since we don't have any service layer validation for post api
// the unit tests do not feature the missing input/validation checks
// instead we will do those DTO/annotation validation (@NotNull, @Size, etc.) → Test in integration/controller tests.
@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
  @Mock
  PostRepository postRepository;

  @Mock
  UserRepository userRepository;

  @Mock
  PostSearchDao postSearchDao;

  @Mock
  SecurityContext securityContext;

  @Mock
  Authentication authentication;

  @InjectMocks
  PostService postService;

  // returning our mock securityContext object:
  @BeforeEach
  void setUp() {
    SecurityContextHolder.setContext(securityContext);
  }

  private PostRequest validPostRequest() {
    PostRequest postRequest = new PostRequest();
    List<String> screenshots = new ArrayList<>(Arrays.asList("FIRST PIC", "SECOND PIC"));
    postRequest.setTitle("TEST POST");
    postRequest.setTagline("TEST TAGLINE");
    postRequest.setCategory(Category.AI);
    postRequest.setStage(Stage.DEVELOPMENT);
    postRequest.setProblemDescription("FACING PROBLEM");
    postRequest.setSolutionDescription("VERY GOOD SOLUTION");
    postRequest.setScreenshotUrls(screenshots);
    postRequest.setLiveDemoUrl("FIND HERE ON WWW.GOOGLE.COM");

    return postRequest;
  };

  private User mockUser() {
    User user = new User();
    user.setId(1L);
    user.setUsername("JohnDoe123");
    user.setEmail("johndoe2@gmail.com");
    user.setPassword("hashedPass");
    return user;
  }

  private Post mockPost() {
    Post post = new Post();
    List<String> screenshots = new ArrayList<>(Arrays.asList("FIRST PIC", "SECOND PIC"));
    post.setId(1L);
    post.setAuthor(mockUser());
    post.setTitle("Test Post");
    post.setTagline("Test Tagline");
    post.setCategory(Category.AI);
    post.setStage(Stage.DEVELOPMENT);
    post.setProblemDescription("Test problem");
    post.setSolutionDescription("Test solution");
    post.setScreenshotUrls(screenshots);
    post.setLiveDemoUrl("FIND HERE ON WWW.GOOGLE.COM");
    post.setCreatedAt(Instant.now());
    post.setUpdatedAt(Instant.now());
    return post;
  }

  // create post test endpoints:
  // user not found/ not authenticated and tries to make post
  // successfull post creation with only required fields, optionals left blank
  // successfull post creation with all fields including optionals
  @Test
  void userNotFoundWhenMakingPost() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");
    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> postService.createPost(validPostRequest()));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertEquals("Unable to get username", ex.getReason());

    verify(postRepository, never()).save(any(Post.class));
  }

  @Test
  void successfullPostCreationWithOnlyRequiredFields() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    User user = mockUser();
    Post savedPost = mockPost();

    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.of(user));
    when(postRepository.save(any(Post.class))).thenReturn(mockPost());

    PostRequest minimalRequest = new PostRequest();
    minimalRequest.setTitle("TEST POST");
    minimalRequest.setTagline("TEST TAGLINE");
    minimalRequest.setCategory(Category.AI);
    minimalRequest.setStage(Stage.DEVELOPMENT);
    minimalRequest.setProblemDescription("FACING PROBLEM");
    minimalRequest.setSolutionDescription("VERY GOOD SOLUTION");

    PostResponse response = postService.createPost(minimalRequest);
    assertEquals(savedPost.getId(), response.id());
    assertEquals(savedPost.getTitle(), response.title());
    assertEquals(savedPost.getTagline(), response.tagline());
    assertEquals(savedPost.getCategory(), response.category());
    assertEquals(savedPost.getStage(), response.stage());
    assertEquals(savedPost.getProblemDescription(), response.problemDescription());
    assertEquals(savedPost.getSolutionDescription(), response.solutionDescription());
  }

  @Test
  void successfullPostCreationWithAllFields() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("JohnDoe123");

    User user = mockUser();
    Post savedPost = mockPost();

    when(userRepository.findByUsername("JohnDoe123")).thenReturn(Optional.of(user));
    when(postRepository.save(any(Post.class))).thenReturn(mockPost());

    PostResponse response = postService.createPost(validPostRequest());

    assertNotNull(response);
    assertEquals(savedPost.getId(), response.id());
    assertEquals(savedPost.getTitle(), response.title());
    assertEquals(savedPost.getTagline(), response.tagline());
    assertEquals(savedPost.getCategory(), response.category());
    assertEquals(savedPost.getStage(), response.stage());
    assertEquals(savedPost.getProblemDescription(), response.problemDescription());
    assertEquals(savedPost.getSolutionDescription(), response.solutionDescription());
    assertEquals(savedPost.getAuthor(), response.author());
    assertEquals(savedPost.getScreenshotUrls(), response.screenshotUrls());
    assertEquals(savedPost.getLiveDemoUrl(), response.liveDemoUrl());
    assertEquals(savedPost.isBoosted(), false);

    verify(postRepository, times(1)).save(any(Post.class));
    verify(userRepository, times(1)).save(user);
  }

  // get post by id test endpoints:
  // post not found endpoint
  // successfull post fetch

  // get all posts test endpoints:
  // get posts without any filters
  // get all posts filtered by category
  // get all posts with search query

  // update post test endpoints:
  // partial update (only some fields provided)
  // unauthorised user trying to update
  // post not found exception
  // successfull full update

  // delete post test endpoints:
  // unauthorised user trying to delete post
  // post not found exception
  // successfull post deletion

}
