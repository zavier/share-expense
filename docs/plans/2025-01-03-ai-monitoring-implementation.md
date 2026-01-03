# AI模块监控系统实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为AI模块添加性能监控功能，记录每次大模型调用的响应时间、Token使用量、成功率等指标，并提供查询统计API。

**Architecture:** 基于Spring AI Advisor的无侵入式监控方案，通过ThreadLocal传递上下文，自动拦截ChatClient调用并记录监控数据到MySQL数据库。

**Tech Stack:** Spring AI, Spring Data JPA, MySQL 8.0+, ThreadLocal, Advisor模式

---

## Task 1: 创建数据库表

**Files:**
- Create: `share-expense-infrastructure/src/main/resources/db/migration/V2025__create_ai_monitoring_log.sql` (or `scripts/ai_monitoring.sql` if not using Flyway)

**Step 1: 创建SQL脚本文件**

```sql
-- 文件: share-expense-infrastructure/src/main/resources/scripts/ai_monitoring.sql

CREATE TABLE IF NOT EXISTS ai_monitoring_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    model_name VARCHAR(50) NOT NULL COMMENT '模型名称(deepseek-chat/LongCat-Flash-Chat)',
    call_type VARCHAR(20) NOT NULL COMMENT '调用类型(CHAT/SUGGESTION)',

    -- 性能指标
    start_time DATETIME NOT NULL COMMENT '调用开始时间',
    end_time DATETIME NOT NULL COMMENT '调用结束时间',
    latency_ms BIGINT NOT NULL COMMENT '响应耗时(毫秒)',

    -- Token使用量
    prompt_tokens INT DEFAULT NULL COMMENT '输入token数',
    completion_tokens INT DEFAULT NULL COMMENT '输出token数',
    total_tokens INT DEFAULT NULL COMMENT '总token数',

    -- 调用状态
    status VARCHAR(20) NOT NULL COMMENT '调用状态(SUCCESS/FAILURE/TIMEOUT)',
    error_type VARCHAR(100) DEFAULT NULL COMMENT '错误类型',
    error_message TEXT DEFAULT NULL COMMENT '错误详情',

    -- 请求/响应摘要
    user_message_preview VARCHAR(500) DEFAULT NULL COMMENT '用户消息摘要',
    assistant_message_preview VARCHAR(500) DEFAULT NULL COMMENT 'AI响应摘要',

    -- 元数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',

    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_model_name (model_name),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status),
    INDEX idx_user_time (user_id, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用监控日志表';
```

**Step 2: 执行SQL创建表**

Run:
```bash
mysql -u root -p share_expense < share-expense-infrastructure/src/main/resources/scripts/ai_monitoring.sql
```

Expected:
```
# No output means success
```

**Step 3: 验证表已创建**

Run:
```bash
mysql -u root -p share_expense -e "SHOW CREATE TABLE ai_monitoring_log\G"
```

Expected:
```
Table: ai_monitoring_log
Create Table: CREATE TABLE `ai_monitoring_log` (...)
```

**Step 4: Commit**

```bash
git add share-expense-infrastructure/src/main/resources/scripts/ai_monitoring.sql
git commit -m "feat(monitoring): create ai_monitoring_log table"
```

---

## Task 2: 创建JPA实体类

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/entity/AiMonitoringLogEntity.java`
- Reference: `share-expense-ai/src/main/java/com/github/zavier/ai/entity/AiSessionEntity.java` (参考现有实体写法)

**Step 1: 创建实体类目录和文件**

Run:
```bash
mkdir -p share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/entity
```

Create file `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/entity/AiMonitoringLogEntity.java`:

```java
package com.github.zavier.ai.monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_monitoring_log", indexes = {
    @Index(name = "idx_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_model_name", columnList = "model_name"),
    @Index(name = "idx_start_time", columnList = "start_time"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_user_time", columnList = "user_id,start_time")
})
public class AiMonitoringLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    @Column(name = "call_type", nullable = false, length = 20)
    private String callType;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "user_message_preview", length = 500)
    private String userMessagePreview;

    @Column(name = "assistant_message_preview", length = 500)
    private String assistantMessagePreview;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

**Step 2: 验证编译**

Run:
```bash
cd share-expense-ai && mvn compile -q
```

Expected: No errors

**Step 3: Commit**

```bash
git add share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/entity/AiMonitoringLogEntity.java
git commit -m "feat(monitoring): add AiMonitoringLogEntity JPA entity"
```

---

## Task 3: 创建Repository接口

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/repository/AiMonitoringRepository.java`
- Reference: `share-expense-ai/src/main/java/com/github/zavier/ai/repository/AiSessionRepository.java`

**Step 1: 创建repository目录和接口**

Run:
```bash
mkdir -p share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/repository
```

Create file `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/repository/AiMonitoringRepository.java`:

```java
package com.github.zavier.ai.monitoring.repository;

import com.github.zavier.ai.monitoring.entity.AiMonitoringLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
    Object[] getBasicStatistics(
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
```

**Step 2: 验证编译**

Run:
```bash
cd share-expense-ai && mvn compile -q
```

Expected: No errors

**Step 3: Commit**

```bash
git add share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/repository/AiMonitoringRepository.java
git commit -m "feat(monitoring): add AiMonitoringRepository with statistics queries"
```

---

## Task 4: 创建DTO类

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/dto/AiMonitoringLogDto.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/dto/PerformanceStatisticsDto.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/dto/ErrorAnalysisDto.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/dto/TrendDataDto.java`

**Step 1: 创建dto目录**

Run:
```bash
mkdir -p share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/dto
```

**Step 2: 创建AiMonitoringLogDto**

Create file `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/dto/AiMonitoringLogDto.java`:

```java
package com.github.zavier.ai.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMonitoringLogDto {
    private Long id;
    private String conversationId;
    private String modelName;
    private String callType;
    private LocalDateTime startTime;
    private Long latencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String status;
    private String errorType;
    private String errorMessage;
    private String userMessagePreview;
    private String assistantMessagePreview;
}
```

**Step 3: 创建PerformanceStatisticsDto**

Create file `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/dto/PerformanceStatisticsDto.java`:

```java
package com.github.zavier.ai.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceStatisticsDto {
    // 调用次数
    private Long totalCalls;
    private Long successCalls;
    private Long failureCalls;
    private Double successRate;

    // 性能指标
    private Double avgLatencyMs;
    private Long minLatencyMs;
    private Long maxLatencyMs;
    private Double p50LatencyMs;
    private Double p90LatencyMs;
    private Double p99LatencyMs;

    // Token统计
    private Long totalPromptTokens;
    private Long totalCompletionTokens;
    private Long totalTokens;

    // 错误分析
    private List<ErrorAnalysisDto> errorBreakdown;
}
```

**Step 4: 创建ErrorAnalysisDto**

Create file `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/dto/ErrorAnalysisDto.java`:

```java
package com.github.zavier.ai.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorAnalysisDto {
    private String errorType;
    private Long count;
    private Double percentage;
    private String exampleMessage;
}
```

**Step 5: 创建TrendDataDto**

Create file `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/dto/TrendDataDto.java`:

```java
package com.github.zavier.ai.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataDto {
    private LocalDateTime timeBucket;
    private Long callCount;
    private Double avgLatency;
    private Long totalTokens;
}
```

**Step 6: 验证编译**

Run:
```bash
cd share-expense-ai && mvn compile -q
```

Expected: No errors

**Step 7: Commit**

```bash
git add share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/dto/
git commit -m "feat(monitoring): add monitoring DTOs (Log, Statistics, Error, Trend)"
```

---

## Task 5: 创建AiCallContext工具类

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/context/AiCallContext.java`
- Reference: `share-expense-ai/src/main/java/com/github/zavier/web/filter/UserHolder.java` (获取当前用户)

**Step 1: 创建context目录和工具类**

Run:
```bash
mkdir -p share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/context
```

Create file `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/context/AiCallContext.java`:

```java
package com.github.zavier.ai.monitoring.context;

import com.github.zavier.web.filter.UserHolder;

/**
 * AI调用上下文容器（ThreadLocal）
 * 用于在业务层和Advisor之间传递调用信息
 */
public class AiCallContext {

    private static final ThreadLocal<CallInfo> CONTEXT = new ThreadLocal<>();

    /**
     * 设置调用上下文（自动获取当前用户）
     */
    public static void setContext(String conversationId, CallType callType) {
        Integer userId = getCurrentUserId();
        CONTEXT.set(new CallInfo(conversationId, callType, userId));
    }

    /**
     * 获取调用上下文
     */
    public static CallInfo get() {
        return CONTEXT.get();
    }

    /**
     * 清理调用上下文（必须调用，避免内存泄漏）
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前登录用户ID
     */
    private static Integer getCurrentUserId() {
        if (UserHolder.getUser() == null) {
            return null;
        }
        return UserHolder.getUser().getUserId();
    }

    /**
     * 调用信息记录
     */
    public record CallInfo(
            String conversationId,
            CallType callType,
            Integer userId
    ) {}

    /**
     * 调用类型枚举
     */
    public enum CallType {
        CHAT,       // 普通聊天
        SUGGESTION  // 建议生成
    }
}
```

**Step 2: 验证编译**

Run:
```bash
cd share-expense-ai && mvn compile -q
```

Expected: No errors

**Step 3: Commit**

```bash
git add share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/context/AiCallContext.java
git commit -m "feat(monitoring): add AiCallContext for ThreadLocal context propagation"
```

---

## Task 6: 创建AiMonitoringAdvisor拦截器

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/advisor/AiMonitoringAdvisor.java`
- Reference: `share-expense-ai/src/main/java/com/github/zavier/ai/impl/AiChatServiceImpl.java` (理解ChatClient调用方式)

**Step 1: 创建advisor目录和拦截器**

Run:
```bash
mkdir -p share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/advisor
```

Create file `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/advisor/AiMonitoringAdvisor.java`:

```java
package com.github.zavier.ai.monitoring.advisor;

import com.github.zavier.ai.monitoring.context.AiCallContext;
import com.github.zavier.ai.monitoring.service.AiMonitoringService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

/**
 * AI调用监控拦截器
 * 自动拦截所有ChatClient调用并记录监控数据
 */
@Slf4j
@Component
public class AiMonitoringAdvisor implements CallAroundAdvisor {

    @Resource
    private AiMonitoringService monitoringService;

    @Override
    public String getName() {
        return "AiMonitoringAdvisor";
    }

    @Override
    public ChatAroundAdvisorResponse aroundCall(ChatAroundAdvisorRequest request, CallAroundAdvisorChain chain) {
        final long startTime = System.currentTimeMillis();

        try {
            // 执行AI调用
            ChatResponse response = chain.nextAroundCall(request).chatResponse();

            // 记录成功调用
            long latency = System.currentTimeMillis() - startTime;
            AiCallContext.CallInfo callInfo = AiCallContext.get();

            if (callInfo != null) {
                try {
                    monitoringService.recordSuccess(
                            callInfo.conversationId(),
                            callInfo.callType(),
                            callInfo.userId(),
                            request,
                            response,
                            latency
                    );
                } catch (Exception e) {
                    // 监控记录失败不影响业务
                    log.error("[AI监控] 记录成功调用失败", e);
                }
            } else {
                log.warn("[AI监控] 未找到调用上下文，跳过记录");
            }

            return response;

        } catch (Exception e) {
            // 记录失败调用
            long latency = System.currentTimeMillis() - startTime;
            AiCallContext.CallInfo callInfo = AiCallContext.get();

            if (callInfo != null) {
                try {
                    monitoringService.recordFailure(
                            callInfo.conversationId(),
                            callInfo.callType(),
                            callInfo.userId(),
                            request,
                            e,
                            latency
                    );
                } catch (Exception ex) {
                    // 监控记录失败不影响业务
                    log.error("[AI监控] 记录失败调用失败", ex);
                }
            }

            throw e;
        }
    }
}
```

**Step 2: 修正API兼容性问题**

注意：Spring AI的API可能有所不同，需要检查实际API。如果编译失败，修改为:

```java
@Override
public AdvisedRequest aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
    // 实现同上，但返回类型改为AdvisedRequest
}
```

**Step 3: 验证编译**

Run:
```bash
cd share-expense-ai && mvn compile -q
```

Expected: 可能有编译错误（因为AiMonitoringService还没创建），这是正常的

**Step 4: 暂存文件**

```bash
git add share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/advisor/AiMonitoringAdvisor.java
```

---

## Task 7: 创建AiMonitoringService服务类

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/service/AiMonitoringService.java`
- Modify: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/advisor/AiMonitoringAdvisor.java` (完成上一步的commit)

**Step 1: 创建service目录**

Run:
```bash
mkdir -p share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/service
```

**Step 2: 创建AiMonitoringService**

Create file `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/service/AiMonitoringService.java`:

```java
package com.github.zavier.ai.monitoring.service;

import com.github.zavier.ai.monitoring.context.AiCallContext;
import com.github.zavier.ai.monitoring.dto.AiMonitoringLogDto;
import com.github.zavier.ai.monitoring.dto.ErrorAnalysisDto;
import com.github.zavier.ai.monitoring.dto.PerformanceStatisticsDto;
import com.github.zavier.ai.monitoring.entity.AiMonitoringLogEntity;
import com.github.zavier.ai.monitoring.repository.AiMonitoringRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatOptions;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI监控业务服务
 */
@Slf4j
@Service
public class AiMonitoringService {

    @Resource
    private AiMonitoringRepository monitoringRepository;

    /**
     * 记录成功的AI调用
     */
    @Transactional
    public void recordSuccess(String conversationId, AiCallContext.CallType callType,
                              Integer userId, Object request, ChatResponse response,
                              long latency) {
        AiMonitoringLogEntity entity = buildBaseEntity(conversationId, callType, userId, request);
        entity.setLatencyMs(latency);
        entity.setStatus("SUCCESS");

        // 提取token使用量
        if (response != null && response.getMetadata() != null) {
            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                entity.setPromptTokens(usage.getPromptTokens());
                entity.setCompletionTokens(usage.getCompletionTokens());
                entity.setTotalTokens(usage.getTotalTokens());
            }
        }

        // 提取响应摘要
        if (response != null && response.getResult() != null) {
            String content = response.getResult().getOutput().getContent();
            entity.setAssistantMessagePreview(truncate(content, 500));
        }

        monitoringRepository.save(entity);
        log.debug("[AI监控] 记录成功调用: conversationId={}, latency={}ms", conversationId, latency);
    }

    /**
     * 记录失败的AI调用
     */
    @Transactional
    public void recordFailure(String conversationId, AiCallContext.CallType callType,
                              Integer userId, Object request, Exception e, long latency) {
        AiMonitoringLogEntity entity = buildBaseEntity(conversationId, callType, userId, request);
        entity.setLatencyMs(latency);
        entity.setStatus(mapExceptionToStatus(e));
        entity.setErrorType(e.getClass().getSimpleName());
        entity.setErrorMessage(truncate(e.getMessage(), 1000));

        monitoringRepository.save(entity);
        log.debug("[AI监控] 记录失败调用: conversationId={}, status={}, error={}",
                conversationId, entity.getStatus(), e.getClass().getSimpleName());
    }

    /**
     * 查询会话调用历史（带用户权限验证）
     */
    public Page<AiMonitoringLogEntity> getCallHistory(String conversationId, Integer userId, Pageable pageable) {
        return monitoringRepository.findByConversationIdAndUserIdOrderByStartTimeDesc(
                conversationId, userId, pageable
        );
    }

    /**
     * 获取性能统计数据（带用户权限验证）
     */
    public PerformanceStatisticsDto getStatistics(LocalDateTime start, LocalDateTime end,
                                                   String model, String status, String callType, Integer userId) {
        // 获取基础统计
        List<Object[]> basicStats = monitoringRepository.getBasicStatistics(
                userId, start, end, model, status, callType
        );

        // 获取百分位数
        List<Object[]> percentiles = monitoringRepository.getPercentileLatency(
                userId, start, end, model, status, callType
        );

        // 获取错误分析
        List<Object[]> errors = monitoringRepository.getErrorAnalysis(userId, start, end);

        // 组装结果（简化版，实际需要处理null和数据转换）
        return PerformanceStatisticsDto.builder()
                .totalCalls(extractLong(basicStats.get(0)[0]))
                .avgLatencyMs(extractDouble(basicStats.get(0)[1]))
                .minLatencyMs(extractLong(basicStats.get(0)[2]))
                .maxLatencyMs(extractLong(basicStats.get(0)[3]))
                .totalTokens(extractLong(basicStats.get(0)[4]))
                .successCalls(extractLong(basicStats.get(0)[5]))
                .build();
    }

    /**
     * 获取错误分析
     */
    public List<ErrorAnalysisDto> getErrorAnalysis(LocalDateTime start, LocalDateTime end, Integer userId) {
        List<Object[]> errors = monitoringRepository.getErrorAnalysis(userId, start, end);

        return errors.stream()
                .map(row -> ErrorAnalysisDto.builder()
                        .errorType((String) row[0])
                        .count((Long) row[1])
                        .exampleMessage(truncate((String) row[2], 200))
                        .build())
                .toList();
    }

    // ==================== 辅助方法 ====================

    private AiMonitoringLogEntity buildBaseEntity(String conversationId,
                                                   AiCallContext.CallType callType,
                                                   Integer userId,
                                                   Object request) {
        LocalDateTime now = LocalDateTime.now();

        // 尝试从request提取模型名称
        String modelName = "unknown";
        String userMessagePreview = null;

        if (request != null) {
            try {
                // 这里需要根据实际的request类型来提取信息
                // 暂时使用默认值
            } catch (Exception e) {
                log.warn("[AI监控] 提取请求信息失败", e);
            }
        }

        return AiMonitoringLogEntity.builder()
                .conversationId(conversationId)
                .userId(userId != null ? userId : -1)  // -1表示匿名用户
                .modelName(modelName)
                .callType(callType != null ? callType.name() : "UNKNOWN")
                .startTime(now)
                .endTime(now)
                .userMessagePreview(userMessagePreview)
                .build();
    }

    private String mapExceptionToStatus(Exception e) {
        String className = e.getClass().getSimpleName();
        if (className.contains("Timeout")) {
            return "TIMEOUT";
        } else if (className.contains("RateLimit")) {
            return "RATE_LIMIT";
        } else {
            return "FAILURE";
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    private Long extractLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Double) return ((Double) obj).longValue();
        return 0L;
    }

    private Double extractDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Long) return ((Long) obj).doubleValue();
        if (obj instanceof Integer) return ((Integer) obj).doubleValue();
        return 0.0;
    }
}
```

**Step 3: 完成Advisor的commit**

Run:
```bash
git commit -m "feat(monitoring): add AiMonitoringService with record and query methods"
```

**Step 4: 验证编译**

Run:
```bash
cd share-expense-ai && mvn compile -q
```

Expected: 可能有一些小错误需要修复（API兼容性），但整体结构正确

---

## Task 8: 修改AiChatServiceImpl添加监控上下文

**Files:**
- Modify: `share-expense-ai/src/main/java/com/github/zavier/ai/impl/AiChatServiceImpl.java:212-225` (callAi方法)
- Add import: `import com.github.zavier.ai.monitoring.context.AiCallContext;`

**Step 1: 添加import**

在文件顶部添加:
```java
import com.github.zavier.ai.monitoring.context.AiCallContext;
```

**Step 2: 修改callAi方法**

找到 `callAi` 方法（约212-225行），修改为:

```java
/**
 * 调用 AI 处理
 */
public String callAi(String conversationId) {
    // 设置监控上下文
    AiCallContext.setContext(conversationId, AiCallContext.CallType.CHAT);

    try {
        List<Message> messages = messagePersister.findAllByConversationId(conversationId);

        log.debug("[AI聊天] 调用AI, conversationId={}, 历史消息数={}", conversationId, messages.size());

        String response = chatClient.prompt()
                .messages(messages)
                .call()
                .content();

        log.debug("[AI聊天] AI响应完成, conversationId={}, reply={}", conversationId, response);

        return response;

    } finally {
        // 清理ThreadLocal（必须执行）
        AiCallContext.clear();
    }
}
```

**Step 3: 验证编译**

Run:
```bash
cd share-expense-ai && mvn compile -q
```

Expected: No errors

**Step 4: Commit**

```bash
git add share-expense-ai/src/main/java/com/github/zavier/ai/impl/AiChatServiceImpl.java
git commit -m "feat(monitoring): add monitoring context to AiChatServiceImpl.callAi()"
```

---

## Task 9: 修改SuggestionGenerator添加监控上下文

**Files:**
- Modify: `share-expense-ai/src/main/java/com/github/zavier/ai/service/SuggestionGenerator.java:45-73` (generate方法)
- Add import: `import com.github.zavier.ai.monitoring.context.AiCallContext;`

**Step 1: 添加import**

在文件顶部添加:
```java
import com.github.zavier.ai.monitoring.context.AiCallContext;
```

**Step 2: 修改generate方法**

找到 `generate` 方法（约45-73行），修改为:

```java
/**
 * 根据对话历史生成建议
 *
 * @param history 对话历史
 * @param conversationId 会话ID
 * @return 建议列表
 */
public List<SuggestionItem> generate(List<ConversationEntity> history, String conversationId) {
    boolean isNewUser = history.isEmpty();

    // 构建上下文
    List<Message> recentMessages = buildRecentMessages(history);

    // 设置监控上下文
    AiCallContext.setContext(conversationId, AiCallContext.CallType.SUGGESTION);

    try {
        // 调用 AI 生成建议
        final List<SuggestionItem> suggestionList = suggestionChatClient.prompt()
                .messages(recentMessages)
                .call()
                .entity(new ParameterizedTypeReference<List<SuggestionItem>>() {});

        log.info("[建议生成] AI响应: conversationId={}, suggestionList={}", conversationId, suggestionList);

        // 如果解析失败或没有建议，返回默认建议
        if (CollectionUtils.isEmpty(suggestionList)) {
            log.warn("[建议生成] AI响应为空，使用默认建议");
            return getDefaultSuggestions(isNewUser);
        }

        // 最多返回 5 个建议
        return suggestionList.stream().limit(5).toList();

    } catch (Exception e) {
        log.error("[建议生成] AI生成失败，使用默认建议", e);
        return getDefaultSuggestions(isNewUser);

    } finally {
        // 清理ThreadLocal（必须执行）
        AiCallContext.clear();
    }
}
```

**Step 3: 验证编译**

Run:
```bash
cd share-expense-ai/src/main/java/com/github/zavier/ai && mvn compile -q
```

Expected: No errors

**Step 4: Commit**

```bash
git add share-expense-ai/src/main/java/com/github/zavier/ai/service/SuggestionGenerator.java
git commit -m "feat(monitoring): add monitoring context to SuggestionGenerator.generate()"
```

---

## Task 10: 创建AiMonitoringController查询接口

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/controller/AiMonitoringController.java`
- Reference: `share-expense-ai/src/main/java/com/github/zavier/ai/AiChatController.java` (参考Controller写法)

**Step 1: 创建controller目录**

Run:
```bash
mkdir -p share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/controller
```

**Step 2: 创建Controller**

Create file `share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/controller/AiMonitoringController.java`:

```java
package com.github.zavier.ai.monitoring.controller;

import com.github.zavier.ai.common.Result;
import com.github.zavier.ai.exception.AuthenticationException;
import com.github.zavier.ai.monitoring.dto.AiMonitoringLogDto;
import com.github.zavier.ai.monitoring.dto.ErrorAnalysisDto;
import com.github.zavier.ai.monitoring.dto.PerformanceStatisticsDto;
import com.github.zavier.ai.monitoring.entity.AiMonitoringLogEntity;
import com.github.zavier.ai.monitoring.service.AiMonitoringService;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI监控查询接口
 */
@RestController
@RequestMapping("/api/ai/monitoring")
public class AiMonitoringController {

    @Resource
    private AiMonitoringService monitoringService;

    /**
     * 1. 查询会话调用历史
     */
    @GetMapping("/session/{conversationId}/history")
    public Result<Page<AiMonitoringLogDto>> getCallHistory(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Integer currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());

        Page<AiMonitoringLogEntity> entityPage = monitoringService.getCallHistory(
                conversationId, currentUserId, pageable
        );

        Page<AiMonitoringLogDto> dtoPage = entityPage.map(this::toDto);
        return Result.success(dtoPage);
    }

    /**
     * 2. 性能统计报表
     */
    @GetMapping("/statistics")
    public Result<PerformanceStatisticsDto> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String callType) {

        Integer currentUserId = getCurrentUserId();
        PerformanceStatisticsDto statistics = monitoringService.getStatistics(
                startTime, endTime, model, status, callType, currentUserId
        );

        return Result.success(statistics);
    }

    /**
     * 3. 错误分析
     */
    @GetMapping("/errors/analysis")
    public Result<List<ErrorAnalysisDto>> getErrorAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        Integer currentUserId = getCurrentUserId();
        List<ErrorAnalysisDto> errors = monitoringService.getErrorAnalysis(
                startTime, endTime, currentUserId
        );

        return Result.success(errors);
    }

    /**
     * 4. 用户概览（最近7天）
     */
    @GetMapping("/overview")
    public Result<PerformanceStatisticsDto> getUserOverview() {
        Integer currentUserId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        PerformanceStatisticsDto statistics = monitoringService.getStatistics(
                sevenDaysAgo, now, null, null, null, currentUserId
        );

        return Result.success(statistics);
    }

    /**
     * 获取当前登录用户ID
     */
    private Integer getCurrentUserId() {
        if (UserHolder.getUser() == null) {
            throw new AuthenticationException("用户未登录");
        }
        return UserHolder.getUser().getUserId();
    }

    /**
     * 实体转DTO
     */
    private AiMonitoringLogDto toDto(AiMonitoringLogEntity entity) {
        return AiMonitoringLogDto.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .modelName(entity.getModelName())
                .callType(entity.getCallType())
                .startTime(entity.getStartTime())
                .latencyMs(entity.getLatencyMs())
                .promptTokens(entity.getPromptTokens())
                .completionTokens(entity.getCompletionTokens())
                .totalTokens(entity.getTotalTokens())
                .status(entity.getStatus())
                .errorType(entity.getErrorType())
                .errorMessage(entity.getErrorMessage())
                .userMessagePreview(entity.getUserMessagePreview())
                .assistantMessagePreview(entity.getAssistantMessagePreview())
                .build();
    }
}
```

**Step 3: 验证编译**

Run:
```bash
cd start && mvn compile -q
```

Expected: No errors

**Step 4: Commit**

```bash
git add share-expense-ai/src/main/java/com/github/zavier/ai/monitoring/controller/AiMonitoringController.java
git commit -m "feat(monitoring): add AiMonitoringController with query APIs"
```

---

## Task 11: 编写集成测试

**Files:**
- Create: `share-expense-ai/src/test/java/com/github/zavier/ai/monitoring/AiMonitoringIntegrationTest.java`
- Reference: `share-expense-ai/src/test/java/com/github/zavier/ai/` (参考现有测试)

**Step 1: 创建测试文件**

Create file `share-expense-ai/src/test/java/com/github/zavier/ai/monitoring/AiMonitoringIntegrationTest.java`:

```java
package com.github.zavier.ai.monitoring;

import com.github.zavier.ai.monitoring.entity.AiMonitoringLogEntity;
import com.github.zavier.ai.monitoring.repository.AiMonitoringRepository;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI监控集成测试
 */
@SpringBootTest
public class AiMonitoringIntegrationTest {

    @Resource
    private AiMonitoringRepository monitoringRepository;

    @Test
    public void testSaveAndQueryMonitoringLog() {
        // 创建测试数据
        AiMonitoringLogEntity entity = AiMonitoringLogEntity.builder()
                .conversationId("test-conversation-123")
                .userId(1)
                .modelName("deepseek-chat")
                .callType("CHAT")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .latencyMs(1500L)
                .status("SUCCESS")
                .totalTokens(100)
                .build();

        // 保存
        AiMonitoringLogEntity saved = monitoringRepository.save(entity);
        assertNotNull(saved.getId());

        // 查询
        var found = monitoringRepository.findByConversationIdAndUserIdOrderByStartTimeDesc(
                "test-conversation-123", 1, null
        );

        assertTrue(found.getContent().stream()
                .anyMatch(e -> e.getConversationId().equals("test-conversation-123")));
    }
}
```

**Step 2: 运行测试**

Run:
```bash
cd share-expense-ai && mvn test -Dtest=AiMonitoringIntegrationTest
```

Expected: PASS

**Step 3: Commit**

```bash
git add share-expense-ai/src/test/java/com/github/zavier/ai/monitoring/AiMonitoringIntegrationTest.java
git commit -m "test(monitoring): add integration test for monitoring log"
```

---

## Task 12: 手动测试验证

**Step 1: 启动应用**

Run:
```bash
cd start && mvn spring-boot:run
```

Expected: 应用正常启动在端口8081

**Step 2: 触发AI调用**

使用前端或API发送AI聊天请求，确保会话创建并记录监控数据

**Step 3: 查询监控数据**

Run:
```bash
curl -X GET "http://localhost:8081/api/ai/monitoring/session/{conversationId}/history?page=0&size=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Expected: 返回监控记录列表

**Step 4: 查询统计数据**

Run:
```bash
curl -X GET "http://localhost:8081/api/ai/monitoring/overview" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Expected: 返回最近7天的统计数据

**Step 5: 验证数据库**

Run:
```bash
mysql -u root -p share_expense -e "SELECT COUNT(*) FROM ai_monitoring_log"
```

Expected: 返回记录数量 > 0

**Step 6: Commit (if any fixes needed)**

如果有bug修复，提交修复

---

## Task 13: 更新文档

**Files:**
- Modify: `README.md` (添加监控功能说明)
- Modify: `CLAUDE.md` (添加监控模块说明)

**Step 1: 更新README**

在README中添加监控功能说明:

```markdown
## AI监控功能

系统自动记录所有AI调用的性能数据，包括：
- 响应时间（平均值、P50/P90/P99）
- Token使用量
- 成功率统计
- 错误分析

### API接口

- `GET /api/ai/monitoring/session/{id}/history` - 查询会话调用历史
- `GET /api/ai/monitoring/statistics` - 性能统计报表
- `GET /api/ai/monitoring/errors/analysis` - 错误分析
- `GET /api/ai/monitoring/overview` - 用户概览（最近7天）

### 数据表

- `ai_monitoring_log`: 存储每次AI调用的监控数据
```

**Step 2: 更新CLAUDE.md**

在CLAUDE.md中添加:

```markdown
## AI监控模块

**Components:**
- `AiMonitoringAdvisor`: 自动拦截ChatClient调用
- `AiCallContext`: ThreadLocal上下文传递
- `AiMonitoringService`: 监控数据记录和查询
- `AiMonitoringController`: RESTful查询API

**Usage:**
- 业务代码只需添加 `AiCallContext.setContext()` 和 `clear()`
- 监控数据自动记录，无需手动调用
```

**Step 3: Commit**

```bash
git add README.md CLAUDE.md
git commit -m "docs(monitoring): update documentation for monitoring feature"
```

---

## Task 14: 最终验证和总结

**Step 1: 完整测试套件**

Run:
```bash
cd start && mvn test
```

Expected: 所有测试通过

**Step 2: 代码审查**

Review checklist:
- [ ] 所有文件都有正确的package和import
- [ ] 数据库表索引正确
- [ ] ThreadLocal在使用后正确清理（finally块）
- [ ] 用户权限验证在所有查询接口中
- [ ] 异常处理不影响业务流程（try-catch in advisor）
- [ ] 日志级别适当（debug用于详细日志，info用于关键信息）

**Step 3: 性能验证**

- 启动应用，发送100次AI请求
- 检查监控数据是否完整记录
- 确认响应时间无明显增加

**Step 4: Final commit**

```bash
git status
git add .
git commit -m "feat(monitoring): complete AI monitoring system implementation"
```

**Step 5: Create PR (if in feature branch)**

```bash
git push origin feature/ai-monitoring
```

然后在GitHub创建Pull Request

---

## 实施检查清单

完成每个任务后勾选：

- [ ] Task 1: 创建数据库表
- [ ] Task 2: 创建JPA实体类
- [ ] Task 3: 创建Repository接口
- [ ] Task 4: 创建DTO类
- [ ] Task 5: 创建AiCallContext工具类
- [ ] Task 6: 创建AiMonitoringAdvisor拦截器
- [ ] Task 7: 创建AiMonitoringService服务类
- [ ] Task 8: 修改AiChatServiceImpl添加监控上下文
- [ ] Task 9: 修改SuggestionGenerator添加监控上下文
- [ ] Task 10: 创建AiMonitoringController查询接口
- [ ] Task 11: 编写集成测试
- [ ] Task 12: 手动测试验证
- [ ] Task 13: 更新文档
- [ ] Task 14: 最终验证和总结

---

## 常见问题和解决方案

**Q1: Spring AI Advisor API不兼容**
A: 检查项目中Spring AI版本，调整接口方法签名

**Q2: ThreadLocal内存泄漏**
A: 确保所有setContext()调用都有对应的clear()在finally块中

**Q3: Token数据为null**
A: 某些模型可能不返回token信息，需要做null处理

**Q4: 百分位数查询失败**
A: 确认MySQL版本 >= 8.0，否则需要移除PERCENTILE_CONT查询

**Q5: 用户隔离失效**
A: 检查所有Repository查询是否包含userId条件

---

**实施完成后，系统将具备完整的AI监控能力，包括性能分析、成本追踪和错误诊断。**
