package com.github.zavier.ai.impl;

import com.github.zavier.ai.*;
import com.github.zavier.ai.domain.MessageRole;
import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import com.github.zavier.ai.dto.SuggestionsResponse;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.function.*;
import com.github.zavier.ai.provider.AiPromptProvider;
import com.github.zavier.ai.service.ChatModelProvider;
import com.github.zavier.ai.service.MessagePersister;
import com.github.zavier.ai.service.SuggestionGenerator;
import com.github.zavier.ai.validator.ChatRequestValidator;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * AI 聊天服务实现（重构版）
 *
 * 设计原则：
 * - 单一职责：专注于聊天流程编排，具体职责委托给专门的服务
 * - 依赖倒置：依赖抽象的服务接口，而非具体实现
 * - 开闭原则：通过组件化设计，便于扩展新功能
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private ChatClient chatClient;

    @Resource
    private ChatModelProvider chatModelProvider;

    @Resource
    private AiSessionService aiSessionService;

    // AI 工具函数（7个）
    @Resource
    private CreateProjectFunction createProjectFunction;

    @Resource
    private AddMembersFunction addMembersFunction;

    @Resource
    private AddExpenseRecordFunction addExpenseRecordFunction;

    @Resource
    private GetSettlementFunction getSettlementFunction;

    @Resource
    private ListProjectsFunction listProjectsFunction;

    @Resource
    private GetProjectDetailsFunction getProjectDetailsFunction;

    @Resource
    private GetExpenseDetailsFunction getExpenseDetailsFunction;

    // 辅助服务组件
    @Resource
    private AiPromptProvider promptProvider;

    @Resource
    private MessagePersister messagePersister;

    @Resource
    private ChatRequestValidator requestValidator;

    @Resource
    private SuggestionGenerator suggestionGenerator;

    @PostConstruct
    public void init() {
        this.chatClient = ChatClient.builder(chatModelProvider.selectChatModel())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem(promptProvider.getChatSystemPrompt())
                .defaultTools(
                        createProjectFunction,
                        addMembersFunction,
                        addExpenseRecordFunction,
                        getSettlementFunction,
                        listProjectsFunction,
                        getProjectDetailsFunction,
                        getExpenseDetailsFunction
                )
                .build();

        log.info("[AI聊天服务] 初始化完成");
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        // 1. 准备会话上下文
        ChatContext context = prepareChatContext(request);

        log.info("[AI聊天] 收到请求, conversationId={}, userId={}, message={}",
            context.conversationId(), context.userId(), request.message());

        // 2. 请求验证（速率限制 + 意图验证）
        ChatRequestValidator.ValidationResult validationResult =
            requestValidator.validate(request, context.conversationId(), context.userId());

        if (validationResult.isRejected()) {
            handleRejection(context, request, validationResult);
            return validationResult.toResponse(context.conversationId());
        }

        // 3. 保存用户消息
        messagePersister.save(context.conversationId(), MessageRole.USER, request.message());

        // 4. 确保会话存在
        if (context.isNewConversation()) {
            aiSessionService.ensureSessionExists(context.conversationId(), request.message());
        }

        // 5. 调用 AI 处理
        String response = callAi(context.conversationId());

        // 6. 保存 AI 回复并更新会话
        messagePersister.save(context.conversationId(), MessageRole.ASSISTANT, response);
        aiSessionService.updateSessionTimestamp(context.conversationId());

        log.info("[AI聊天] 处理完成, conversationId={}", context.conversationId());

        return AiChatResponse.builder()
            .conversationId(context.conversationId())
            .reply(response)
            .build();
    }

    @Override
    public SuggestionsResponse getSuggestions(String conversationId) {
        log.debug("[AI建议] 生成建议开始, conversationId={}", conversationId);

        // 获取对话历史
        List<ConversationEntity> history = conversationId != null && !conversationId.isBlank()
            ? messagePersister.findEntitiesByConversationId(conversationId)
            : List.of();

        // 生成建议
        List<SuggestionGenerator.SuggestionItem> items =
            suggestionGenerator.generate(history, conversationId);

        log.debug("[AI建议] 生成完成, conversationId={}, count={}", conversationId, items.size());

        // 转换为响应格式
        List<SuggestionsResponse.Suggestion> suggestions = items.stream()
            .map(item -> new SuggestionsResponse.Suggestion(
                item.text(),
                item.type(),
                item.description()
            ))
            .toList();

        return SuggestionsResponse.builder()
            .conversationId(conversationId)
            .suggestions(suggestions)
            .build();
    }

    /**
     * 准备聊天上下文
     */
    private ChatContext prepareChatContext(AiChatRequest request) {
        String conversationId = request.conversationId();
        boolean isNewConversation = false;

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
            isNewConversation = true;
        }

        return new ChatContext(conversationId, getCurrentUserId(), isNewConversation);
    }

    /**
     * 处理拒绝情况
     */
    private void handleRejection(
        ChatContext context,
        AiChatRequest request,
        ChatRequestValidator.ValidationResult result
    ) {
        // 保存用户消息
        messagePersister.save(context.conversationId(), MessageRole.USER, request.message());

        // 如果需要，保存拒绝消息
        if (result.saveRejection()) {
            messagePersister.save(context.conversationId(), MessageRole.ASSISTANT, result.rejectionMessage());
        }
    }

    /**
     * 调用 AI 处理
     */
    public String callAi(String conversationId) {
        List<Message> messages = messagePersister.findAllByConversationId(conversationId);

        log.debug("[AI聊天] 调用AI, conversationId={}, 历史消息数={}", conversationId, messages.size());

        String response = chatClient.prompt()
            .messages(messages)
            .call()
            .content();

        log.debug("[AI聊天] AI响应完成, conversationId={}, reply={}", conversationId, response);

        return response;
    }

    /**
     * 获取当前用户 ID
     */
    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }

    /**
     * 聊天上下文
     */
    private record ChatContext(
        String conversationId,
        Integer userId,
        boolean isNewConversation
    ) {}
}
