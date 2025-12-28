package com.github.zavier.ai.impl;

import com.github.zavier.ai.*;
import com.github.zavier.ai.dto.*;
import com.github.zavier.ai.dto.SuggestionsResponse.Suggestion;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.function.AddExpenseRecordFunction;
import com.github.zavier.ai.function.AddMembersFunction;
import com.github.zavier.ai.function.CreateProjectFunction;
import com.github.zavier.ai.function.GetProjectDetailsFunction;
import com.github.zavier.ai.function.GetSettlementFunction;
import com.github.zavier.ai.function.ListProjectsFunction;
import com.github.zavier.ai.impl.AiSessionServiceImpl;
import com.github.zavier.ai.repository.ConversationRepository;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 聊天服务实现
 * 使用 Spring AI 的 Tool Calling 功能实现与业务逻辑的对接
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private ChatClient chatClient;

    private ChatClient suggestionChatClient;

    @Resource
    private ChatModel chatModel;

    @Resource
    private ConversationRepository conversationRepository;

    @Resource
    private AiSessionServiceImpl aiSessionService;

    // 注入所有工具类
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

    @PostConstruct
    public void init() {
        chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(
                        createProjectFunction,
                        addMembersFunction,
                        addExpenseRecordFunction,
                        getSettlementFunction,
                        listProjectsFunction,
                        getProjectDetailsFunction
                )
                .build();

        suggestionChatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    private static final String SYSTEM_PROMPT = """
        你是一个费用分摊记账助手。你可以帮助用户：
        1. 创建费用分摊项目
        2. 向项目添加成员
        3. 记录费用支出
        4. 查询项目列表
        5. 查询项目详情
        6. 查询结算情况

        **重要提示：**
        - 查询结算时，优先使用项目名称（getSettlementByName），而不是项目ID
        - 如果用户提到项目名称但工具需要项目ID，先调用 listProjects 查找项目
        - 只有当用户明确知道项目ID时，才使用 getSettlement
        - 当需要添加费用记录或添加成员时，如果不确定项目的成员信息，先调用 getProjectDetails 获取项目详情

        请用友好、简洁的中文回复。
        如果信息不完整，对于非关键字段如费用类型等先主动猜测一下，可以不打扰用户。
        涉及金额等信息如果不明确，请主动询问用户，一定要避免资金的错误
        """;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String conversationId = request.conversationId();
        boolean isNewConversation = false;

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
            isNewConversation = true;
        }

        log.info("[AI聊天] 收到用户消息, conversationId={}, userId={}, message={}",
            conversationId, getCurrentUserId(), request.message());

        // 保存用户消息
        saveMessage(conversationId, "user", request.message());

        // 如果是新会话，确保会话记录存在
        if (isNewConversation) {
            aiSessionService.ensureSessionExists(conversationId, request.message());
        }

        // 获取对话历史
        List<Message> messages = buildMessages(conversationId);

        // 调用 AI，使用 tools() 方法注册工具
        // Spring AI 会自动处理工具调用、执行和结果返回
        log.debug("[AI聊天] 调用 AI 处理, conversationId={}, 历史消息数={}", conversationId, messages.size() - 1);
        String response = chatClient.prompt()
            .messages(messages)
            .call()
            .content();

        log.info("[AI聊天] AI 回复完成, conversationId={}, reply={}", conversationId, response);

        // 保存 AI 回复
        saveMessage(conversationId, "assistant", response);

        // 更新会话时间戳
        aiSessionService.updateSessionTimestamp(conversationId);

        return AiChatResponse.builder()
            .conversationId(conversationId)
            .reply(response)
            .build();
    }

    @Override
    public SuggestionsResponse getSuggestions(String conversationId) {
        List<ConversationEntity> history = new ArrayList<>();
        if (conversationId != null && !conversationId.isBlank()) {
            history = conversationRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        }

        List<SuggestionsResponse.Suggestion> suggestions = generateSuggestionsWithAI(history, conversationId);
        log.debug("[AI建议] AI生成建议, conversationId={}, 建议数={}", conversationId, suggestions.size());

        return SuggestionsResponse.builder()
            .conversationId(conversationId)
            .suggestions(suggestions)
            .build();
    }

    /**
     * 使用 AI 根据对话历史生成智能建议
     */
    private List<Suggestion> generateSuggestionsWithAI(List<ConversationEntity> history, String conversationId) {
        // 构建上下文摘要
        String contextSummary = buildContextSummary(history);

        // 构建提示词
        String suggestionPrompt = buildSuggestionPrompt(contextSummary, history.isEmpty());

        try {
            // 调用 AI 生成建议
            final List<Suggestion> suggestionList = suggestionChatClient.prompt()
                    .user(suggestionPrompt)
                    .call()
                    .entity(new ParameterizedTypeReference<List<Suggestion>>() {
                    });

            log.info("[AI建议] AI原始响应: {}", suggestionList);

            // 如果解析失败或没有建议，返回默认建议
            if (CollectionUtils.isEmpty(suggestionList)) {
                log.warn("[AI建议] 解析AI响应失败，使用默认建议");
                return getDefaultSuggestions(false);
            }

            // 最多返回 5 个建议
            return suggestionList.stream().limit(5).toList();
        } catch (Exception e) {
            log.error("[AI建议] AI生成失败，使用默认建议", e);
            return getDefaultSuggestions(history.isEmpty());
        }
    }

    /**
     * 构建上下文摘要
     */
    private String buildContextSummary(List<ConversationEntity> history) {
        if (history.isEmpty()) {
            return "新用户，无历史对话";
        }

        // 只取最近 5 条消息作为上下文
        int start = Math.max(0, history.size() - 5);
        List<ConversationEntity> recentHistory = history.subList(start, history.size());

        StringBuilder summary = new StringBuilder("最近对话：\n");
        for (ConversationEntity entity : recentHistory) {
            String role = "user".equals(entity.getRole()) ? "用户" : "助手";
            summary.append(role).append(": ").append(entity.getContent()).append("\n");
        }
        return summary.toString();
    }

    /**
     * 构建建议生成提示词
     */
    private String buildSuggestionPrompt(String contextSummary, boolean isNewUser) {
        String basePrompt = """
            你是一个费用分摊记账助手。请根据以下对话上下文，判断用户后续可能要进行的操作
            为用户生成 3-4 个智能建议，便于用户快捷使用

            %s

            请生成建议，让用户可以快速继续操作。返回格式必须是纯文本，每行一个建议，格式为：
            建议内容

            只返回建议内容，不要添加任何其他解释。

            可用的操作类型参考：
            - 创建项目：createProject
            - 添加成员：addMembers
            - 记录费用：addExpenseRecord
            - 查询项目列表：listProjects
            - 查询结算：getSettlementByName

            示例建议：
            创建项目「周末聚餐」，成员有张三、李四、王五
            记录今天午饭AA，80元4个人分，张三付的钱
            查询「周末聚餐」的结算情况
            """;

        if (isNewUser) {
            return String.format(basePrompt + "\n\n这是一个新用户，建议引导他们开始使用核心功能。", contextSummary);
        }

        return String.format(basePrompt, contextSummary);
    }

    /**
     * 获取默认建议（降级方案）
     */
    private List<SuggestionsResponse.Suggestion> getDefaultSuggestions(boolean isNewUser) {
        List<SuggestionsResponse.Suggestion> suggestions = new ArrayList<>();

        if (isNewUser) {
            suggestions.add(Suggestion.builder()
                .text("创建项目「周末聚餐」，成员有小明、小红、小李")
                .type("create_project")
                .description("创建新的费用分摊项目")
                .build());
            suggestions.add(Suggestion.builder()
                .text("今天午饭AA，80元4个人分,小明出钱")
                .type("add_expense")
                .description("快速记录一笔支出")
                .build());
            suggestions.add(Suggestion.builder()
                .text("查看我的项目")
                .type("list_projects")
                .description("查看所有费用项目")
                .build());
            suggestions.add(Suggestion.builder()
                .text("查询「周末聚餐」的结算情况")
                .type("query_settlement")
                .description("查看费用分摊情况")
                .build());
        } else {
            suggestions.add(Suggestion.builder()
                .text("查看我的项目列表")
                .type("list_projects")
                .description("查看所有项目")
                .build());
            suggestions.add(Suggestion.builder()
                .text("记录一笔费用")
                .type("add_expense")
                .description("记录支出")
                .build());
            suggestions.add(Suggestion.builder()
                .text("查询结算情况")
                .type("query_settlement")
                .description("查看分摊")
                .build());
            suggestions.add(Suggestion.builder()
                .text("创建新项目")
                .type("create_project")
                .description("新建项目")
                .build());
        }

        return suggestions;
    }

    /**
     * 构建对话消息列表
     */
    private List<Message> buildMessages(String conversationId) {
        List<Message> messages = new ArrayList<>();
        List<ConversationEntity> history = conversationRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        for (ConversationEntity entity : history) {
            if ("user".equals(entity.getRole())) {
                messages.add(new UserMessage(entity.getContent()));
            } else if ("assistant".equals(entity.getRole())) {
                messages.add(new AssistantMessage(entity.getContent()));
            }
        }

        return messages;
    }

    /**
     * 保存消息到数据库
     */
    private void saveMessage(String conversationId, String role, String content) {
        ConversationEntity entity = ConversationEntity.builder()
            .conversationId(conversationId)
            .userId(getCurrentUserId())
            .role(role)
            .content(content)
            .createdAt(LocalDateTime.now())
            .build();

        conversationRepository.save(entity);
    }

    /**
     * 获取当前用户 ID
     */
    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
