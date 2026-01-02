package com.github.zavier.ai.dto;

import lombok.Builder;

import java.util.List;

/**
 * AI 建议响应
 */
@Builder
public record SuggestionsResponse(
        String conversationId,
        List<Suggestion> suggestions
) {
    /**
     * 建议项
     */
    public record Suggestion(
            /**
             * 建议文本
             */
            String text
    ) {
    }
}
