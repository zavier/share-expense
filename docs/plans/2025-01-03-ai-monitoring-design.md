# AI模块监控指标系统设计文档

**设计日期**: 2025-01-03
**设计目标**: 为AI模块调用大模型添加性能监控与分析功能
**方案类型**: 基于Spring AI Advisor的无侵入式监控方案

---

## 1. 需求概述

### 1.1 监控目标
- **主要目标**: 性能监控与分析
- **核心指标**:
  - 响应时间（每次API调用的耗时）
  - 调用状态（成功/失败/超时）
  - Token使用量（输入/输出/总数）
  - 错误类型（超时/限流/API错误等）

### 1.2 功能需求
- **数据粒度**: 单次调用级别（记录每次调用的详细信息）
- **数据存储**: 持久化到数据库
- **查询功能**:
  - 会话历史查询（查看单个会话的AI调用历史）
  - 条件筛选查询（按时间/模型/状态筛选）
  - 性能统计报表（平均值、P99、成功率等）
- **权限控制**: 用户只能查询自己的监控数据

### 1.3 技术约束
- **最小代码修改**: 仅修改2个业务文件（AiChatServiceImpl、SuggestionGenerator）
- **零侵入**: 通过Advisor自动拦截，不影响现有业务逻辑
- **用户隔离**: 强制按用户ID过滤数据

---

## 2. 整体架构设计

### 2.1 核心组件分层

```
┌─────────────────────────────────────────────────────────┐
│  业务层 (Business Layer)                                 │
│  - AiChatServiceImpl                                     │
│  - SuggestionGenerator                                   │
│  （仅需添加 AiCallContext.setContext/clear）             │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  拦截层 (Advisor Layer)                                  │
│  - AiCallContext (ThreadLocal上下文)                     │
│  - AiMonitoringAdvisor (自动拦截所有ChatClient调用)      │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  服务层 (Service Layer)                                  │
│  - AiMonitoringService (记录、查询、统计)                │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  数据层 (Repository Layer)                               │
│  - AiMonitoringRepository (Spring Data JPA)              │
│  - ai_monitoring_log表                                   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  API层 (Controller Layer)                                │
│  - AiMonitoringController (RESTful查询接口)              │
└─────────────────────────────────────────────────────────┘
```

### 2.2 技术选型
- **拦截机制**: Spring AI `CallAroundAdvisor`
- **上下文传递**: `ThreadLocal`
- **数据访问**: Spring Data JPA
- **数据库**: MySQL 8.0+ (支持PERCENTILE_CONT函数)

---

## 3. 数据库设计

### 3.1 表结构

```sql
CREATE TABLE ai_monitoring_log (
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
    error_type VARCHAR(100) DEFAULT NULL COMMENT '错误类型(如RATE_LIMIT/TIMEOUT/API_ERROR)',
    error_message TEXT DEFAULT NULL COMMENT '错误详情',

    -- 请求/响应摘要
    user_message_preview VARCHAR(500) DEFAULT NULL COMMENT '用户消息摘要(前500字符)',
    assistant_message_preview VARCHAR(500) DEFAULT NULL COMMENT 'AI响应摘要(前500字符)',

    -- 元数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',

    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_model_name (model_name),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status),
    INDEX idx_user_time (user_id, start_time)
) COMMENT='AI调用监控日志表';
```

### 3.2 索引说明
- `idx_conversation_id`: 按会话查询调用历史
- `idx_user_id`: 用户数据隔离
- `idx_model_name`: 按模型筛选统计
- `idx_start_time`: 时间范围查询
- `idx_status`: 成功率分析
- `idx_user_time`: 组合查询优化（用户+时间）

---

## 4. 核心组件设计

### 4.1 上下文容器（AiCallContext）

```java
public class AiCallContext {
    private static final ThreadLocal<CallInfo> CONTEXT = new ThreadLocal<>();

    public static void setContext(String conversationId, CallType callType) {
        Integer userId = getCurrentUserId();
        CONTEXT.set(new CallInfo(conversationId, callType, userId));
    }

    public static CallInfo get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    private static Integer getCurrentUserId() {
        if (UserHolder.getUser() == null) {
            return null;
        }
        return UserHolder.getUser().getUserId();
    }

    public record CallInfo(
        String conversationId,
        CallType callType,
        Integer userId
    ) {}

    public enum CallType {
        CHAT,       // 普通聊天
        SUGGESTION  // 建议生成
    }
}
```

### 4.2 监控拦截器（AiMonitoringAdvisor）

```java
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
    public AdvisedRequest aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        final long startTime = System.currentTimeMillis();

        try {
            // 执行AI调用
            ChatResponse response = chain.nextAroundCall(request).chatResponse();

            // 记录成功调用
            long latency = System.currentTimeMillis() - startTime;
            AiCallContext.CallInfo callInfo = AiCallContext.get();

            if (callInfo != null) {
                monitoringService.recordSuccess(
                    callInfo.conversationId(),
                    callInfo.callType(),
                    callInfo.userId(),
                    request,
                    response,
                    latency
                );
            }

            return response;

        } catch (Exception e) {
            // 记录失败调用
            long latency = System.currentTimeMillis() - startTime;
            AiCallContext.CallInfo callInfo = AiCallContext.get();

            if (callInfo != null) {
                monitoringService.recordFailure(
                    callInfo.conversationId(),
                    callInfo.callType(),
                    callInfo.userId(),
                    request,
                    e,
                    latency
                );
            }

            throw e;
        }
    }
}
```

### 4.3 监控服务（AiMonitoringService）

```java
@Service
public class AiMonitoringService {

    @Resource
    private AiMonitoringRepository monitoringRepository;

    /**
     * 记录成功的AI调用
     */
    @Transactional
    public void recordSuccess(String conversationId, AiCallContext.CallType callType,
                             Integer userId, AdvisedRequest request, ChatResponse response,
                             long latency) {
        AiMonitoringLogEntity entity = buildBaseEntity(conversationId, callType, userId, request);
        entity.setLatencyMs(latency);
        entity.setStatus("SUCCESS");

        // 提取token使用量
        if (response != null && response.getMetadata() != null) {
            Usage usage = response.getMetadata().getUsage();
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
    }

    /**
     * 记录失败的AI调用
     */
    @Transactional
    public void recordFailure(String conversationId, AiCallContext.CallType callType,
                             Integer userId, AdvisedRequest request, Exception e, long latency) {
        AiMonitoringLogEntity entity = buildBaseEntity(conversationId, callType, userId, request);
        entity.setLatencyMs(latency);
        entity.setStatus(mapExceptionToStatus(e));
        entity.setErrorType(e.getClass().getSimpleName());
        entity.setErrorMessage(truncate(e.getMessage(), 1000));

        monitoringRepository.save(entity);
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
        // 实现统计查询逻辑
    }

    // 辅助方法...
}
```

### 4.4 数据仓储（AiMonitoringRepository）

```java
public interface AiMonitoringRepository extends JpaRepository<AiMonitoringLogEntity, Long> {

    // 查询指定会话和用户的监控记录
    Page<AiMonitoringLogEntity> findByConversationIdAndUserIdOrderByStartTimeDesc(
        String conversationId, Integer userId, Pageable pageable
    );

    // 统计查询 - 强制包含用户ID
    @Query("""
        SELECT new com.github.zavier.ai.dto.PerformanceStatisticsDto(...)
        FROM AiMonitoringLogEntity e
        WHERE e.userId = :userId
        AND e.startTime BETWEEN :start AND :end
        AND (:model IS NULL OR e.modelName = :model)
        AND (:status IS NULL OR e.status = :status)
        AND (:callType IS NULL OR e.callType = :callType)
    """)
    PerformanceStatisticsDto getStatistics(...);

    // 百分位数查询（P50/P90/P99）
    @Query(value = """
        SELECT
            PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY latency_ms) as p50,
            PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY latency_ms) as p90,
            PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY latency_ms) as p99
        FROM ai_monitoring_log
        WHERE user_id = :userId AND start_time BETWEEN :start AND :end
    """, nativeQuery = true)
    Map<String, Double> getPercentileLatency(...);
}
```

---

## 5. 数据流与集成

### 5.1 完整调用链

```
1. 用户发起AI聊天请求
   └─> AiChatController.chat()
       └─> AiChatService.chat()

2. 业务层设置监控上下文
   AiChatServiceImpl.callAi(conversationId)
   ├─> AiCallContext.setContext(conversationId, CallType.CHAT)
   └─> try {
         chatClient.prompt().messages(messages).call()
       } finally {
         AiCallContext.clear()
       }

3. Advisor自动拦截（无需修改业务代码）
   AiMonitoringAdvisor.aroundCall()
   ├─> 记录开始时间
   ├─> chain.nextAroundCall()  ──> 执行实际AI调用
   │   └─> DeepSeek/LongCat API
   ├─> 从ThreadLocal获取: AiCallContext.get()
   │   - conversationId
   │   - callType (CHAT/SUGGESTION)
   │   - userId (从UserHolder获取)
   ├─> monitoringService.recordSuccess()
   │   └─> monitoringRepository.save(entity)
   │       └─> INSERT INTO ai_monitoring_log
   └─> 返回响应给用户

4. 业务层清理上下文
   └─> AiCallContext.clear() (finally块)
```

### 5.2 业务代码修改点

**仅需要修改2个文件，每个文件约3行代码：**

**修改1: AiChatServiceImpl.callAi()**
```java
public String callAi(String conversationId) {
    // 新增：设置监控上下文
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
        // 新增：清理ThreadLocal
        AiCallContext.clear();
    }
}
```

**修改2: SuggestionGenerator.generate()**
```java
public List<SuggestionItem> generate(List<ConversationEntity> history, String conversationId) {
    boolean isNewUser = history.isEmpty();
    List<Message> recentMessages = buildRecentMessages(history);

    // 新增：设置监控上下文
    AiCallContext.setContext(conversationId, AiCallContext.CallType.SUGGESTION);

    try {
        final List<SuggestionItem> suggestionList = suggestionChatClient.prompt()
            .messages(recentMessages)
            .call()
            .entity(new ParameterizedTypeReference<List<SuggestionItem>>() {});

        log.info("[建议生成] AI响应: conversationId={}, suggestionList={}", conversationId, suggestionList);

        if (CollectionUtils.isEmpty(suggestionList)) {
            log.warn("[建议生成] AI响应为空，使用默认建议");
            return getDefaultSuggestions(isNewUser);
        }

        return suggestionList.stream().limit(5).toList();

    } catch (Exception e) {
        log.error("[建议生成] AI生成失败，使用默认建议", e);
        return getDefaultSuggestions(isNewUser);

    } finally {
        // 新增：清理ThreadLocal
        AiCallContext.clear();
    }
}
```

### 5.3 自动配置

Advisor是`@Component`，会自动注册到Spring容器，并通过Advisor机制自动应用到所有ChatClient调用，无需额外配置。

---

## 6. 查询与统计API

### 6.1 DTO设计

```java
// 监控日志DTO
@Data
@Builder
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

// 性能统计DTO
@Data
@Builder
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

    // 趋势数据
    private List<TrendDataDto> trendData;
}

// 错误分析DTO
@Data
@Builder
public class ErrorAnalysisDto {
    private String errorType;
    private Long count;
    private Double percentage;
    private String exampleMessage;
}

// 趋势数据DTO
@Data
@Builder
public class TrendDataDto {
    private LocalDateTime timeBucket;
    private Long callCount;
    private Double avgLatency;
    private Long totalTokens;
}
```

### 6.2 API接口

```java
@RestController
@RequestMapping("/api/ai/monitoring")
public class AiMonitoringController {

    // 1. 查询会话调用历史
    @GetMapping("/session/{conversationId}/history")
    public Result<Page<AiMonitoringLogDto>> getCallHistory(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Integer currentUserId = getCurrentUserId();
        Page<AiMonitoringLogEntity> entityPage = monitoringService.getCallHistory(
            conversationId, currentUserId, PageRequest.of(page, size)
        );

        return Result.success(entityPage.map(this::toDto));
    }

    // 2. 性能统计报表
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

    // 3. 错误分析
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

    // 4. 趋势数据（用于图表展示）
    @GetMapping("/trends")
    public Result<List<TrendDataDto>> getTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "HOUR") String interval) {

        Integer currentUserId = getCurrentUserId();
        List<TrendDataDto> trends = monitoringService.getTrends(
            startTime, endTime, interval, currentUserId
        );

        return Result.success(trends);
    }

    // 5. 用户概览（最近7天）
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

    private Integer getCurrentUserId() {
        if (UserHolder.getUser() == null) {
            throw new AuthenticationException("用户未登录");
        }
        return UserHolder.getUser().getUserId();
    }
}
```

---

## 7. 权限控制设计

### 7.1 多层防护

1. **Controller层**: 从`UserHolder`获取当前登录用户ID
2. **Service层**: 强制接收userId参数并传递到Repository
3. **Repository层**: SQL查询WHERE条件强制包含`user_id`

### 7.2 数据隔离示例

```java
// Repository查询始终包含userId
public interface AiMonitoringRepository extends JpaRepository<AiMonitoringLogEntity, Long> {
    // WHERE user_id = :userId 强制过滤
    Page<AiMonitoringLogEntity> findByConversationIdAndUserIdOrderByStartTimeDesc(
        String conversationId, Integer userId, Pageable pageable
    );
}
```

### 7.3 安全原则

- **不信任前端参数**: conversationId等参数仅用于业务逻辑，不用于权限验证
- **基于当前用户**: 所有查询基于`UserHolder.getUser().getUserId()`
- **SQL层隔离**: 通过数据库WHERE条件强制隔离，即使代码漏洞也无法绕过

---

## 8. 实施步骤

### 8.1 开发步骤

1. **创建数据库表**
   ```bash
   mysql -u root -p share_expense < scripts/ai_monitoring.sql
   ```

2. **创建基础组件**
   - `AiCallContext.java` - ThreadLocal上下文
   - `AiMonitoringEntity.java` - JPA实体
   - `AiMonitoringRepository.java` - 数据仓储
   - `AiMonitoringAdvisor.java` - 监控拦截器

3. **创建服务层**
   - `AiMonitoringService.java` - 业务服务
   - `AiMonitoringController.java` - REST API
   - DTO类（监控日志、性能统计、错误分析等）

4. **修改业务代码**
   - `AiChatServiceImpl.callAi()` - 添加上下文设置和清理
   - `SuggestionGenerator.generate()` - 添加上下文设置和清理

5. **测试验证**
   - 单元测试：Advisor拦截逻辑
   - 集成测试：监控数据记录
   - 权限测试：用户数据隔离

### 8.2 文件清单

**新增文件** (约12个):
```
share-expense-ai/src/main/java/com/github/zavier/ai/
├── monitoring/
│   ├── AiCallContext.java
│   ├── advisor/
│   │   └── AiMonitoringAdvisor.java
│   ├── entity/
│   │   └── AiMonitoringLogEntity.java
│   ├── repository/
│   │   └── AiMonitoringRepository.java
│   ├── service/
│   │   └── AiMonitoringService.java
│   ├── controller/
│   │   └── AiMonitoringController.java
│   └── dto/
│       ├── AiMonitoringLogDto.java
│       ├── PerformanceStatisticsDto.java
│       ├── ErrorAnalysisDto.java
│       └── TrendDataDto.java

share-expense-infrastructure/src/main/resources/
└── scripts/
    └── ai_monitoring.sql
```

**修改文件** (2个):
```
share-expense-ai/src/main/java/com/github/zavier/ai/
├── impl/AiChatServiceImpl.java         (+5行)
└── service/SuggestionGenerator.java    (+5行)
```

### 8.3 配置项

无需额外配置，Advisor自动生效。可选配置：
- 日志级别：控制监控日志输出
- 异步写入：使用`@Async`提升性能（可选）

---

## 9. 性能考虑

### 9.1 性能优化

1. **异步写入**（可选）:
   ```java
   @Transactional
   @Async("monitoringExecutor")
   public CompletableFuture<Void> recordSuccess(...) {
       // 异步写入数据库，不阻塞AI响应
   }
   ```

2. **批量写入**（可选）:
   - 使用缓冲队列积累记录
   - 定时批量刷入数据库

3. **索引优化**:
   - 已在表设计中添加复合索引`idx_user_time`
   - 支持常见查询组合

### 9.2 存储估算

假设每天10,000次调用：
- 单条记录约 500 bytes
- 每日存储: 10,000 × 500 = 5MB
- 每月存储: 150MB
- 每年存储: 1.8GB

建议保留3-6个月数据，定期归档历史数据。

---

## 10. 监控指标说明

### 10.1 响应时间指标

- **avgLatencyMs**: 平均响应时间，反映整体性能
- **p50LatencyMs**: 中位数响应时间，50%的调用在此时间内完成
- **p90LatencyMs**: 90分位响应时间，反映尾部延迟
- **p99LatencyMs**: 99分位响应时间，反映最慢情况

### 10.2 成功率指标

- **successRate = (successCalls / totalCalls) × 100%**
- 建议阈值: >95%
- 告警阈值: <90%

### 10.3 Token使用量

- **totalTokens = promptTokens + completionTokens**
- 用于成本分析和预算控制
- 可按时间范围统计总消耗

### 10.4 错误类型分类

常见错误类型：
- `RateLimitException`: API限流
- `TimeoutException`: 请求超时
- `ApiException`: API错误
- `NetworkException`: 网络异常

---

## 11. 后续扩展

### 11.1 可能的增强功能

1. **实时监控大屏**: WebSocket推送实时性能数据
2. **告警规则**: 响应时间过长/错误率过高时发送通知
3. **成本管理**: Token消耗趋势分析和预算预警
4. **模型对比**: 不同模型的性能和成本对比
5. **数据导出**: 支持CSV/Excel导出监控报表

### 11.2 技术演进

- 集成Micrometer + Prometheus（云原生监控）
- 使用ClickHouse等时序数据库优化大规模查询
- 引入OpenTelemetry进行分布式追踪

---

## 12. 总结

本设计方案实现了：
- ✅ **零侵入**: 通过Advisor自动拦截，业务代码仅修改2个文件（各5行）
- ✅ **高性能**: ThreadLocal传递上下文，无额外性能开销
- ✅ **安全隔离**: 多层权限验证，用户只能查询自己的数据
- ✅ **完整监控**: 覆盖响应时间、成功率、Token使用、错误分析
- ✅ **易于扩展**: 模块化设计，便于后续功能增强

**核心优势**:
- 符合Spring AI设计理念，与现有架构完美契合
- 自动覆盖所有ChatClient调用（chat + suggestion）
- 支持同步/异步调用场景
- 提供丰富的查询和统计分析功能
