package com.github.zavier.ai.service;

import com.github.zavier.ai.domain.MessageRole;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.provider.AiPromptProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能建议生成服务
 * 根据对话历史为用户生成操作建议
 */
@Slf4j
@Service
public class SuggestionGenerator {

    private final ChatClient suggestionChatClient;


    public SuggestionGenerator(ChatModelProvider chatModelProvider,
                               AiPromptProvider promptProvider) {
        this.suggestionChatClient = ChatClient.builder(chatModelProvider.selectChatModel())
                .defaultSystem(promptProvider.getSuggestionPrompt())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * 根据对话历史生成建议
     *
     * @param history 对话历史
     * @param conversationId 会话ID
     * @return 建议列表
     */
    public List<SuggestionItem> generate(List<ConversationEntity> history, String conversationId) {
        boolean isNewUser = history.isEmpty();

        // 构建上下文
        List<Message> recentMessages = buildRecentMessages(history);

        try {
            // 调用 AI 生成建议
            final List<SuggestionItem> suggestionList = suggestionChatClient.prompt()
                .messages(recentMessages)
                .call()
                .entity(new ParameterizedTypeReference<List<SuggestionItem>>() {});

            log.info("[建议生成] AI响应: conversationId={}, count={}", conversationId, suggestionList.size());

            // 如果解析失败或没有建议，返回默认建议
            if (CollectionUtils.isEmpty(suggestionList)) {
                log.warn("[建议生成] AI响应为空，使用默认建议");
                return getDefaultSuggestions(isNewUser);
            }

            // 最多返回 5 个建议
            return suggestionList.stream().limit(5).toList();

        } catch (Exception e) {
            log.error("[建议生成] AI生成失败，使用默认建议", e);
            return getDefaultSuggestions(isNewUser);
        }
    }

    /**
     * 构建最近的消息列表（最多5条）
     */
    private List<Message> buildRecentMessages(List<ConversationEntity> history) {
        int start = Math.max(0, history.size() - 5);
        List<ConversationEntity> recentHistory = history.subList(start, history.size());
        List<Message> messages = new ArrayList<>();
        for (ConversationEntity entity : recentHistory) {
            MessageRole role = MessageRole.fromCode(entity.getRole());
            if (role == MessageRole.USER) {
                messages.add(new UserMessage(entity.getContent()));
            } else if (role == MessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(entity.getContent()));
            }
        }

        if (!messages.isEmpty()) {
            if (messages.getLast() instanceof UserMessage) {
                messages.removeLast();
            }
            messages.add(new UserMessage("你觉得我后续可能会和你说什么？或者如何回答你的问题"));
        }

        return messages;
    }

    /**
     * 获取默认建议（降级方案）
     */
    private List<SuggestionItem> getDefaultSuggestions(boolean isNewUser) {
        List<SuggestionItem> suggestions = new ArrayList<>();

        if (isNewUser) {
            suggestions.add(new SuggestionItem(
                "创建项目「周末聚餐」，成员有小明、小红、小李",
                "create_project",
                "创建新的费用分摊项目"
            ));
            suggestions.add(new SuggestionItem(
                "今天午饭AA，80元4个人分,小明出钱",
                "add_expense",
                "快速记录一笔支出"
            ));
            suggestions.add(new SuggestionItem(
                "查看我的项目",
                "list_projects",
                "查看所有费用项目"
            ));
            suggestions.add(new SuggestionItem(
                "查询「周末聚餐」的费用明细",
                "query_expense_details",
                "查看费用详细分析"
            ));
        } else {
            suggestions.add(new SuggestionItem(
                "查看我的项目列表",
                "list_projects",
                "查看所有项目"
            ));
            suggestions.add(new SuggestionItem(
                "记录一笔费用",
                "add_expense",
                "记录支出"
            ));
            suggestions.add(new SuggestionItem(
                "查看费用明细",
                "query_expense_details",
                "查看费用详细分析"
            ));
            suggestions.add(new SuggestionItem(
                "创建新项目",
                "create_project",
                "新建项目"
            ));
        }

        return suggestions;
    }

    /**
     * 建议项
     */
    public record SuggestionItem(
        String text,
        String type,
        String description
    ) {}
}
