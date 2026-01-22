package com.verifico.server.comment.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.verifico.server.comment.CommentRepository;
import com.verifico.server.comment.CommentService;
import com.verifico.server.comment.dto.CommentRequest;
import com.verifico.server.post.PostRepository;
import com.verifico.server.user.UserRepository;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
  @Mock
  CommentRepository commentRepository;

  @Mock
  UserRepository userRepository;

  @Mock
  PostRepository postRepository;

  @Mock
  SecurityContext securityContext;

  @InjectMocks
  CommentService commentService;

  // returning mock securityContext object:
  @BeforeEach
  void setup() {
    SecurityContextHolder.setContext(securityContext);
  }

  private CommentRequest validCommentRequest() {
    CommentRequest commentRequest = new CommentRequest();

    commentRequest.setContent("Wohoo! Congratulations on your acheivement!");

    return commentRequest;
  };
}
