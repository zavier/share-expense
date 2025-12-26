package com.github.zavier.ai.repository;

import com.github.zavier.ai.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    List<ConversationEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    void deleteByConversationIdAndCreatedAtBefore(String conversationId, java.time.LocalDateTime cutoff);
}
