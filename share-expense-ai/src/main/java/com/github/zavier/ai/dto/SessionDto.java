package com.github.zavier.ai.dto;

import java.time.LocalDateTime;

/**
 * AI 会话 DTO
 */
public record SessionDto(
        Long id,
        String conversationId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
