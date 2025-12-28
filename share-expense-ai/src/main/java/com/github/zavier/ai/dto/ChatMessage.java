package com.github.zavier.ai.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 聊天消息
 */
@Builder
public record ChatMessage(
        String id,
        String role,  // user, assistant, system
        String content,
        LocalDateTime timestamp
) {
}
