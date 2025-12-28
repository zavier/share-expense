package com.github.zavier.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * AI 聊天请求
 */
public record AiChatRequest(
        @NotBlank(message = "消息内容不能为空") String message,
        String conversationId
) {
    // 带默认值的构造方法
    public AiChatRequest(String message) {
        this(message, null);
    }
}
