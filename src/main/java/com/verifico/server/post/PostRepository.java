package com.verifico.server.post;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
  Optional<Post> findById(Long id);

  Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<Post> findByCategoryOrderByCreatedAtDesc(Category category, Pageable pageable);
}
