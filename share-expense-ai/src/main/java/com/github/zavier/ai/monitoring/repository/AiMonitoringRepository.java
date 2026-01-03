package com.github.zavier.ai.monitoring.repository;

import com.github.zavier.ai.monitoring.dto.PerformanceStatisticsDto;
import com.github.zavier.ai.monitoring.entity.AiMonitoringLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiMonitoringRepository extends JpaRepository<AiMonitoringLogEntity, Long> {

    /**
     * 查询指定会话和用户的监控记录（分页）
     */
    Page<AiMonitoringLogEntity> findByConversationIdAndUserIdOrderByStartTimeDesc(
            String conversationId, Integer userId, Pageable pageable
    );

    /**
     * 统计查询 - 基础统计
     */
    @Query("""
        SELECT new com.github.zavier.ai.monitoring.dto.PerformanceStatisticsDto(
            COUNT(e),
            AVG(e.latencyMs),
            MIN(e.latencyMs),
            MAX(e.latencyMs),
            SUM(e.totalTokens),
            SUM(CASE WHEN e.status = 'SUCCESS' THEN 1 ELSE 0 END)
        )
        FROM AiMonitoringLogEntity e
        WHERE e.userId = :userId
        AND e.startTime BETWEEN :start AND :end
        AND (:model IS NULL OR e.modelName = :model)
        AND (:status IS NULL OR e.status = :status)
        AND (:callType IS NULL OR e.callType = :callType)
    """)
    PerformanceStatisticsDto getBasicStatistics(
            @Param("userId") Integer userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("model") String model,
            @Param("status") String status,
            @Param("callType") String callType
    );

    /**
     * 百分位数查询（P50/P90/P99）- MySQL 8.0+
     */
    @Query(value = """
        SELECT
            PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY latency_ms) as p50,
            PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY latency_ms) as p90,
            PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY latency_ms) as p99
        FROM ai_monitoring_log
        WHERE user_id = :userId
        AND start_time BETWEEN :start AND :end
        AND (:model IS NULL OR model_name = :model)
        AND (:status IS NULL OR status = :status)
        AND (:callType IS NULL OR call_type = :callType)
        """, nativeQuery = true)
    List<Object[]> getPercentileLatency(
            @Param("userId") Integer userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("model") String model,
            @Param("status") String status,
            @Param("callType") String callType
    );

    /**
     * 错误类型分组统计
     */
    @Query("""
        SELECT e.errorType, COUNT(e), MAX(e.errorMessage)
        FROM AiMonitoringLogEntity e
        WHERE e.userId = :userId
        AND e.startTime BETWEEN :start AND :end
        AND e.status != 'SUCCESS'
        GROUP BY e.errorType
        ORDER BY COUNT(e) DESC
    """)
    List<Object[]> getErrorAnalysis(
            @Param("userId") Integer userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}