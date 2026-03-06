package com.verifico.server.feed_algorithm.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemDto {
    private Long videoId;
    private String title;
    private String description;
    private String videoUrl;
    private List<String> tags;
    private double score;
}
