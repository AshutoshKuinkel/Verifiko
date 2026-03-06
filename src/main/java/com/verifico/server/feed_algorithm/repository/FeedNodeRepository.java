package com.verifico.server.feed_algorithm.repository;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verifico.server.feed_algorithm.model.FeedNode;

public interface FeedNodeRepository extends JpaRepository<FeedNode, Long> {

    Page<FeedNode> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
        SELECT f
        FROM FeedNode f
        WHERE f.active = true
          AND f.id NOT IN :excludedIds
        ORDER BY f.createdAt DESC
        """)
    Page<FeedNode> findActiveExcludingIds(
        @Param("excludedIds") Set<Long> excludedIds,
        Pageable pageable
    );
}
