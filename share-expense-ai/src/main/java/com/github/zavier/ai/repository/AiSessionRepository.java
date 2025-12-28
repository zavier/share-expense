package com.github.zavier.ai.repository;

import com.github.zavier.ai.entity.AiSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI 会话元数据 Repository
 */
@Repository
public interface AiSessionRepository extends JpaRepository<AiSessionEntity, Long> {

    /**
     * 根据用户ID查询会话列表，按创建时间倒序
     */
    List<AiSessionEntity> findByUserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * 根据 conversationId 查询会话
     */
    Optional<AiSessionEntity> findByConversationId(String conversationId);

    /**
     * 根据 conversationId 删除会话
     */
    void deleteByConversationId(String conversationId);
}
