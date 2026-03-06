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
public class SwipeResponse {
    private String sessionId;
    private List<FeedItemDto> items;
    private boolean hasMore;
}
