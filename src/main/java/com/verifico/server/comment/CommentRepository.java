package com.verifico.server.comment;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  @EntityGraph(attributePaths = "author")
  Page<Comment> findAllByPostIdOrderByCreatedAtDesc(Long id,Pageable pageable);
}
