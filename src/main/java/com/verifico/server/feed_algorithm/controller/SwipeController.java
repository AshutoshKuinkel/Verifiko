package com.verifico.server.feed_algorithm.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.verifico.server.feed_algorithm.dto.SwipeEventRequest;
import com.verifico.server.feed_algorithm.dto.SwipeResponse;
import com.verifico.server.feed_algorithm.service.FeedService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class SwipeController {

    private final FeedService feedService;

    /**
     * Endpoint to get the next batch of videos for the swipe feed.
     * GET /api/feed/next?limit=5&sessionId=abc&excludeIds=1,2
     */
    @GetMapping("/next")
    public ResponseEntity<SwipeResponse> getNextBatch(
            Authentication authentication,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Set<Long> excludeIds) {
        Long userId = resolveAuthenticatedUserId(authentication);
        Set<Long> safeExcludes = excludeIds == null ? new HashSet<>() : excludeIds;

        SwipeResponse response = feedService.getFeedForUser(userId, limit, sessionId, safeExcludes);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/swipe-event")
    public ResponseEntity<Void> recordSwipeEvent(
            Authentication authentication,
            @RequestBody SwipeEventRequest request) {
        Long userId = resolveAuthenticatedUserId(authentication);
        feedService.recordSwipeEvent(userId, request);
        return ResponseEntity.ok().build();
    }

    private Long resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found!");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal type");
    }
}
