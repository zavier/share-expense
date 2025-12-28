package com.github.zavier.ai.dto;

import lombok.Builder;

/**
 * AI 聊天响应
 */
@Builder
public record AiChatResponse(
        String conversationId,
        String reply
) {
}
