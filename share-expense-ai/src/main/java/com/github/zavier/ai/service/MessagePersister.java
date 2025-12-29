package com.github.zavier.ai.service;

import com.github.zavier.ai.domain.MessageRole;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.repository.ConversationRepository;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息持久化服务
 * 负责消息的保存和查询操作
 */
@Slf4j
@Service
public class MessagePersister {

    @Resource
    private ConversationRepository conversationRepository;

    /**
     * 保存单条消息
     *
     * @param conversationId 会话ID
     * @param role 消息角色
     * @param content 消息内容
     */
    public void save(String conversationId, MessageRole role, String content) {
        ConversationEntity entity = ConversationEntity.builder()
            .conversationId(conversationId)
            .userId(getCurrentUserId())
            .role(role.getCode())
            .content(content)
            .createdAt(LocalDateTime.now())
            .build();

        conversationRepository.save(entity);
        log.debug("[消息持久化] 保存消息成功, conversationId={}, role={}", conversationId, role);
    }

    /**
     * 批量保存消息
     *
     * @param conversationId 会话ID
     * @param messages 消息列表（角色+内容）
     */
    public void saveBatch(String conversationId, List<MessageRecord> messages) {
        List<ConversationEntity> entities = new ArrayList<>();
        Integer userId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        for (MessageRecord msg : messages) {
            entities.add(ConversationEntity.builder()
                .conversationId(conversationId)
                .userId(userId)
                .role(msg.role().getCode())
                .content(msg.content())
                .createdAt(now)
                .build());
        }

        conversationRepository.saveAll(entities);
        log.debug("[消息持久化] 批量保存消息成功, conversationId={}, count={}", conversationId, messages.size());
    }

    /**
     * 获取会话的所有消息，转换为 Spring AI Message 格式
     *
     * @param conversationId 会话ID
     * @return Spring AI Message 列表
     */
    public List<Message> findAllByConversationId(String conversationId) {
        List<ConversationEntity> history = conversationRepository
            .findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<Message> messages = new ArrayList<>();
        for (ConversationEntity entity : history) {
            MessageRole role = MessageRole.fromCode(entity.getRole());
            if (role == MessageRole.USER) {
                messages.add(new UserMessage(entity.getContent()));
            } else if (role == MessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(entity.getContent()));
            }
        }

        return messages;
    }

    /**
     * 获取会话的所有原始消息实体
     *
     * @param conversationId 会话ID
     * @return 消息实体列表
     */
    public List<ConversationEntity> findEntitiesByConversationId(String conversationId) {
        return conversationRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * 获取当前用户 ID
     */
    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }

    /**
     * 消息记录（用于批量保存）
     */
    public record MessageRecord(MessageRole role, String content) {}
}
