package com.github.zavier.ai.repository;

import com.github.zavier.ai.entity.ConversationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    List<ConversationEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    /**
     * 获取会话的最新N条消息（按时间倒序获取，使用 Pageable 动态指定数量）
     */
    List<ConversationEntity> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    void deleteByConversationIdAndCreatedAtBefore(String conversationId, java.time.LocalDateTime cutoff);
}
