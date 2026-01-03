package com.github.zavier.ai.monitoring.repository;

import com.github.zavier.ai.monitoring.entity.AiMonitoringLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiMonitoringRepository extends JpaRepository<AiMonitoringLogEntity, Long> {

    /**
     * 查询指定会话和用户的监控记录（分页）
     */
    Page<AiMonitoringLogEntity> findByConversationIdAndUserIdOrderByStartTimeDesc(
            String conversationId, Integer userId, Pageable pageable
    );

    /**
     * 查询指定用户的监控记录（分页）- 用户隔离
     */
    Page<AiMonitoringLogEntity> findByUserIdOrderByStartTimeDesc(Integer userId, Pageable pageable);

}