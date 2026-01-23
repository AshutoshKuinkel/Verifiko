package com.verifico.server.comment;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.comment.dto.CommentRequest;
import com.verifico.server.comment.dto.CommentResponse;
import com.verifico.server.post.Post;
import com.verifico.server.post.PostRepository;
import com.verifico.server.user.User;
import com.verifico.server.user.UserRepository;
import com.verifico.server.user.dto.AuthorResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Create, read, delete operations only for now.
// We won't feature edit/update comments initially.
@Service
@RequiredArgsConstructor
public class CommentService {

  private final CommentRepository commentRepository;
  private final UserRepository userRepository;
  private final PostRepository postRepository;

  @Transactional
  public CommentResponse postComment(CommentRequest request, Long id) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();

    // check post exists
    Post post = postRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

    // is person trying to comment authenticated?
    User author = userRepository.findByUsername(username).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User data inconsistency detected"));

    Comment comment = new Comment();
    comment.setPost(post);
    comment.setContent(request.getContent());
    comment.setAuthor(author);

    Comment savedComment = commentRepository.save(comment);
    return toCommentResponse(savedComment);
  }

  public void getAllCommentsForPost() {
    // return a paginated response i.e only load 15 comments at a time.. then
    // implement lazy loading in frontend
  }

  @Transactional
  public void deleteMyComment(Long id) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();

    // check if post exists
    Comment comment = commentRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found!"));

    // check if current user authenticated is the user who commented
    if (!username.equals(comment.getAuthor().getUsername())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "You are not authorised to make changes to this comment!");
    }

    // delete comment
    commentRepository.delete(comment);
  }

  private CommentResponse toCommentResponse(Comment comment) {
    return new CommentResponse(comment.getId(), comment.getContent(), toAuthorResponse(comment.getAuthor()),
        comment.getPost().getId(), comment.getCreatedAt());
  }

  private AuthorResponse toAuthorResponse(User user) {
    return new AuthorResponse(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(),
        user.getAvatarUrl());
  }
}
