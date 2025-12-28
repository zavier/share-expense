package com.github.zavier.ai.dto;

import java.util.List;

/**
 * 会话列表响应
 */
public record SessionListResponse(
        List<SessionDto> sessions
) {
}
