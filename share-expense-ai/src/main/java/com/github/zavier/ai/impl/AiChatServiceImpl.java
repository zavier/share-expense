package com.github.zavier.ai.impl;

import com.github.zavier.ai.*;
import com.github.zavier.ai.dto.*;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.function.AddExpenseRecordFunction;
import com.github.zavier.ai.function.AddMembersFunction;
import com.github.zavier.ai.function.CreateProjectFunction;
import com.github.zavier.ai.function.GetSettlementFunction;
import com.github.zavier.ai.function.ListProjectsFunction;
import com.github.zavier.ai.repository.ConversationRepository;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
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

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private ConversationRepository conversationRepository;

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

    @PostConstruct
    public void init() {
        chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(
                        createProjectFunction,
                        addMembersFunction,
                        addExpenseRecordFunction,
                        getSettlementFunction,
                        listProjectsFunction
                )
                .build();
    }

    private static final String SYSTEM_PROMPT = """
        你是一个费用分摊记账助手。你可以帮助用户：
        1. 创建费用分摊项目
        2. 向项目添加成员
        3. 记录费用支出
        4. 查询项目列表
        5. 查询结算情况

        **重要提示：**
        - 查询结算时，优先使用项目名称（getSettlementByName），而不是项目ID
        - 如果用户提到项目名称但工具需要项目ID，先调用 listProjects 查找项目
        - 只有当用户明确知道项目ID时，才使用 getSettlement

        请用友好、简洁的中文回复。
        如果信息不完整，请主动询问用户。
        """;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        log.info("[AI聊天] 收到用户消息, conversationId={}, userId={}, message={}",
            conversationId, getCurrentUserId(), request.getMessage());

        // 保存用户消息
        saveMessage(conversationId, "user", request.getMessage());

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

        return AiChatResponse.builder()
            .conversationId(conversationId)
            .reply(response)
            .build();
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
