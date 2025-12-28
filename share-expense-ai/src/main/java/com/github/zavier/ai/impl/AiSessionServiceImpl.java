package com.github.zavier.ai.impl;

import com.github.zavier.ai.AiSessionService;
import com.github.zavier.ai.dto.MessageDto;
import com.github.zavier.ai.dto.SessionDto;
import com.github.zavier.ai.entity.AiSessionEntity;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.repository.AiSessionRepository;
import com.github.zavier.ai.repository.ConversationRepository;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 会话管理服务实现
 */
@Slf4j
@Service
public class AiSessionServiceImpl implements AiSessionService {

    @Resource
    private AiSessionRepository sessionRepository;

    @Resource
    private ConversationRepository conversationRepository;

    @Override
    public List<SessionDto> listSessions() {
        Integer userId = getCurrentUserId();
        List<AiSessionEntity> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return sessions.stream()
                .map(this::toSessionDto)
                .collect(Collectors.toList());
    }

    @Override
    public String createSession() {
        String conversationId = UUID.randomUUID().toString();
        log.info("[会话管理] 创建新会话, conversationId={}, userId={}", conversationId, getCurrentUserId());

        // 延迟创建：在用户发送第一条消息时再创建会话记录
        return conversationId;
    }

    @Override
    public String createSession(String title) {
        String conversationId = UUID.randomUUID().toString();
        Integer userId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        AiSessionEntity session = AiSessionEntity.builder()
                .conversationId(conversationId)
                .userId(userId)
                .title(title)
                .createdAt(now)
                .updatedAt(now)
                .build();

        sessionRepository.save(session);
        log.info("[会话管理] 创建新会话（指定标题）, conversationId={}, userId={}, title={}", conversationId, userId, title);

        return conversationId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String conversationId) {
        Integer userId = getCurrentUserId();

        // 检查会话是否存在且属于当前用户
        AiSessionEntity session = sessionRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该会话");
        }

        // 删除会话元数据
        sessionRepository.deleteByConversationId(conversationId);

        // 删除会话的所有消息
        List<ConversationEntity> messages = conversationRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        conversationRepository.deleteAll(messages);

        log.info("[会话管理] 删除会话, conversationId={}, userId={}, 消息数={}", conversationId, userId, messages.size());
    }

    @Override
    public void renameSession(String conversationId, String title) {
        Integer userId = getCurrentUserId();

        AiSessionEntity session = sessionRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该会话");
        }

        session.setTitle(title);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("[会话管理] 重命名会话, conversationId={}, userId={}, newTitle={}", conversationId, userId, title);
    }

    @Override
    public List<MessageDto> getSessionMessages(String conversationId) {
        Integer userId = getCurrentUserId();

        // 验证会话权限
        AiSessionEntity session = sessionRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该会话");
        }

        List<ConversationEntity> entities = conversationRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        return entities.stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    /**
     * 根据用户消息生成会话标题
     * 取前 30 个字符
     */
    public String generateTitleFromMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "新对话";
        }

        // 去除首尾空格
        message = message.trim();

        // 取前 30 个字符
        if (message.length() <= 30) {
            return message;
        }

        return message.substring(0, 30) + "...";
    }

    /**
     * 确保会话记录存在（用于聊天时延迟创建）
     */
    public void ensureSessionExists(String conversationId, String firstMessage) {
        if (sessionRepository.findByConversationId(conversationId).isEmpty()) {
            String title = generateTitleFromMessage(firstMessage);
            Integer userId = getCurrentUserId();
            LocalDateTime now = LocalDateTime.now();

            AiSessionEntity session = AiSessionEntity.builder()
                    .conversationId(conversationId)
                    .userId(userId)
                    .title(title)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            sessionRepository.save(session);
            log.info("[会话管理] 延迟创建会话记录, conversationId={}, userId={}, title={}", conversationId, userId, title);
        }
    }

    /**
     * 更新会话时间戳（用于会话排序）
     */
    public void updateSessionTimestamp(String conversationId) {
        sessionRepository.findByConversationId(conversationId).ifPresent(session -> {
            if (session.getUserId().equals(getCurrentUserId())) {
                session.setUpdatedAt(LocalDateTime.now());
                sessionRepository.save(session);
            }
        });
    }

    private SessionDto toSessionDto(AiSessionEntity entity) {
        return new SessionDto(
                entity.getId(),
                entity.getConversationId(),
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private MessageDto toMessageDto(ConversationEntity entity) {
        return new MessageDto(
                entity.getRole(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
