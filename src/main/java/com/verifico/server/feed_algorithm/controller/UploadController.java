package com.verifico.server.feed_algorithm.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.verifico.server.feed_algorithm.dto.UploadRequest;
import com.verifico.server.feed_algorithm.model.FeedNode;
import com.verifico.server.feed_algorithm.service.FeedService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class UploadController {

    private final FeedService feedService;

    /**
     * Endpoint for users to upload new content
     * POST /api/content/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadNewContent(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "videoUrl", required = false) String videoUrl,
            @RequestParam(value = "tags", required = false) String tagsCsv) {

        // For prototype: file persistence + ML vector extraction can be async worker steps.
        List<String> tags = parseTags(tagsCsv);
        UploadRequest request = new UploadRequest(
            title,
            description,
            videoUrl == null || videoUrl.isBlank() ? "/videos/" + file.getOriginalFilename() : videoUrl,
            tags
        );

        FeedNode saved = feedService.createUploadedNode(request);
        return ResponseEntity.ok("Upload received. Node created with id=" + saved.getId());
    }

    private List<String> parseTags(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tagsCsv.split(","))
            .map(String::trim)
            .filter(tag -> !tag.isBlank())
            .toList();
    }
}
