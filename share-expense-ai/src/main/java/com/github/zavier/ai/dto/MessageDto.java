package com.github.zavier.ai.dto;

import java.time.LocalDateTime;

/**
 * 消息 DTO
 */
public record MessageDto(
        String role,
        String content,
        LocalDateTime createdAt
) {
}
