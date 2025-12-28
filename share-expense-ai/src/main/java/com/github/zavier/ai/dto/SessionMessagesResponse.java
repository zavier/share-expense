package com.github.zavier.ai.dto;

import java.util.List;

/**
 * 会话历史消息响应
 */
public record SessionMessagesResponse(
        List<MessageDto> messages
) {
}
