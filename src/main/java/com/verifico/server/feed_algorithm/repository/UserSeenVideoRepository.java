package com.verifico.server.feed_algorithm.repository;

import java.time.Instant;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verifico.server.feed_algorithm.model.UserSeenVideo;

public interface UserSeenVideoRepository extends JpaRepository<UserSeenVideo, Long> {

    @Query("""
        SELECT s.videoId
        FROM UserSeenVideo s
        WHERE s.userId = :userId
          AND s.servedAt >= :cutoff
        """)
    Set<Long> findRecentlySeenVideoIds(
        @Param("userId") Long userId,
        @Param("cutoff") Instant cutoff
    );

    @Query("""
        SELECT s.videoId
        FROM UserSeenVideo s
        WHERE s.userId = :userId
          AND s.sessionId = :sessionId
        """)
    Set<Long> findSessionSeenVideoIds(
        @Param("userId") Long userId,
        @Param("sessionId") String sessionId
    );
}
