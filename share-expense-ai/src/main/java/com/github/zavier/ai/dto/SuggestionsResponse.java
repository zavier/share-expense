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
    @Builder
    public record Suggestion(
            /**
             * 建议文本
             */
            String text,

            /**
             * 建议类型（用于前端图标选择）
             * create_project, add_members, add_expense, query_settlement, list_projects
             */
            String type,

            /**
             * 建议的简短描述
             */
            String description
    ) {
    }
}
