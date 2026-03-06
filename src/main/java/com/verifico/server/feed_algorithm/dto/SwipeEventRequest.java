package com.verifico.server.feed_algorithm.dto;

import com.verifico.server.feed_algorithm.model.SwipeAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SwipeEventRequest {
    private Long videoId;
    private SwipeAction action;
    private String sessionId;
}
