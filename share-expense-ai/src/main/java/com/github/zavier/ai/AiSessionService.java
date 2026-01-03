package com.github.zavier.ai;

import com.github.zavier.ai.dto.MessageDto;
import com.github.zavier.ai.dto.SessionDto;

import java.util.List;

/**
 * AI 会话管理服务
 */
public interface AiSessionService {

    /**
     * 获取当前用户的所有会话列表
     */
    List<SessionDto> listSessions();

    /**
     * 创建新会话
     * @return conversationId
     */
    String createSession();

    /**
     * 创建新会话并指定标题
     * @return conversationId
     */
    String createSession(String title);

    /**
     * 删除会话及其所有消息
     */
    void deleteSession(String conversationId);

    /**
     * 重命名会话
     */
    void renameSession(String conversationId, String title);

    /**
     * 获取会话的历史消息
     */
    List<MessageDto> getSessionMessages(String conversationId);

    /**
     * 确保会话存在（如果不存在则创建）
     *
     * @param conversationId 会话ID
     * @param firstMessage 第一条消息（用于生成标题）
     */
    void ensureSessionExists(String conversationId, String firstMessage);

    /**
     * 更新会话时间戳
     *
     * @param conversationId 会话ID
     */
    void updateSessionTimestamp(String conversationId);

    /**
     * 验证会话所有权
     *
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @throws IllegalArgumentException 如果会话不存在
     * @throws com.github.zavier.ai.exception.AuthenticationException 如果用户无权访问该会话
     */
    void verifySessionOwnership(String conversationId, Integer userId);
}
