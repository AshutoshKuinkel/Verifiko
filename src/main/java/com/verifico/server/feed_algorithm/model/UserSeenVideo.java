package com.verifico.server.feed_algorithm.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_seen_videos", indexes = {
    @Index(name = "idx_seen_user_served", columnList = "user_id,served_at"),
    @Index(name = "idx_seen_user_session", columnList = "user_id,session_id")
})
@Getter
@Setter
@NoArgsConstructor
public class UserSeenVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "session_id", nullable = false, length = 80)
    private String sessionId;

    @Column(name = "served_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant servedAt = Instant.now();

    public UserSeenVideo(Long userId, Long videoId, String sessionId, Instant servedAt) {
        this.userId = userId;
        this.videoId = videoId;
        this.sessionId = sessionId;
        this.servedAt = servedAt;
    }
}
