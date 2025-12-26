package com.github.zavier.ai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zavier.ai.*;
import com.github.zavier.ai.dto.*;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.function.AiFunctionExecutor;
import com.github.zavier.ai.function.FunctionContext;
import com.github.zavier.ai.repository.ConversationRepository;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private ChatClient chatClient;

    @Resource
    private AiFunctionRegistry functionRegistry;

    @Resource
    private ConversationRepository conversationRepository;

    @Resource
    private ApplicationContext applicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 存储待确认的操作（临时存储，生产环境应使用 Redis）
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();

    // 存储操作与会话的映射
    private final Map<String, String> actionToConversationMap = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
        你是一个费用分摊记账助手。你可以帮助用户：
        1. 创建费用分摊项目
        2. 向项目添加成员
        3. 记录费用支出
        4. 查询结算情况

        请用友好、简洁的中文回复。
        如果信息不完整，请主动询问用户。
        """;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        // 保存用户消息
        saveMessage(conversationId, "user", request.getMessage());

        // 获取对话历史
        List<Message> messages = buildMessages(conversationId);

        // 调用 AI
        // 注意: Spring AI 1.0.0 中的 function calling API 与文档有差异
        // functions() 方法在当前版本中不可用，需要升级到更新版本或使用不同的 API
        // TODO: 实现 function calling 集成
        String response = chatClient.prompt()
            .messages(messages)
            .call()
            .content();

        // 保存 AI 回复
        saveMessage(conversationId, "assistant", response);

        return AiChatResponse.builder()
            .conversationId(conversationId)
            .reply(response)
            .build();
    }

    @Override
    public AiChatResponse confirm(String conversationId, String actionId) {
        PendingAction action = pendingActions.get(actionId);
        if (action == null) {
            throw new RuntimeException("操作已过期或不存在");
        }

        // 执行实际业务逻辑
        String result = executeAction(action);

        // 清除待确认操作
        pendingActions.remove(actionId);
        actionToConversationMap.remove(actionId);

        // 保存执行结果
        saveMessage(conversationId, "assistant", result);

        return AiChatResponse.builder()
            .conversationId(conversationId)
            .reply(result)
            .build();
    }

    @Override
    public AiChatResponse cancel(String conversationId) {
        // 清除该会话的所有待确认操作
        Iterator<Map.Entry<String, String>> iterator = actionToConversationMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (conversationId.equals(entry.getValue())) {
                pendingActions.remove(entry.getKey());
                iterator.remove();
            }
        }

        return AiChatResponse.builder()
            .conversationId(conversationId)
            .reply("操作已取消")
            .build();
    }

    private List<Message> buildMessages(String conversationId) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

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

    private String executeAction(PendingAction action) {
        AiFunctionExecutor executor = functionRegistry.getFunction(action.getActionType());
        if (executor == null) {
            throw new RuntimeException("未知的操作类型: " + action.getActionType());
        }

        FunctionContext context = FunctionContext.builder()
            .userId(getCurrentUserId())
            .build();

        // 将 params 转换为相应的 Request 对象
        Class<?> requestType = functionRegistry.getRequestType(action.getActionType());
        Object request = convertParamsToRequest(action.getParams(), requestType);

        // 执行函数并返回结果
        return executor.execute(request, context);
    }

    /**
     * 使用 Jackson ObjectMapper 将 Map 转换为指定的 Request 类型
     */
    private Object convertParamsToRequest(Map<String, Object> params, Class<?> requestType) {
        try {
            return objectMapper.convertValue(params, requestType);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("参数转换失败: " + e.getMessage(), e);
        }
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
