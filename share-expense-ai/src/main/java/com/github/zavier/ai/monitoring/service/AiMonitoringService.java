package com.github.zavier.ai.monitoring.service;

import com.github.zavier.ai.monitoring.context.AiCallContext.CallType;
import com.github.zavier.ai.monitoring.dto.AiMonitoringLogDto;
import com.github.zavier.ai.monitoring.dto.PerformanceStatisticsDto;
import com.github.zavier.ai.monitoring.dto.ErrorAnalysisDto;
import com.github.zavier.ai.monitoring.dto.TrendDataDto;
import com.github.zavier.ai.monitoring.entity.AiMonitoringLogEntity;
import com.github.zavier.ai.monitoring.repository.AiMonitoringRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI调用监控服务
 * 提供AI调用的记录、统计和分析功能
 */
@Slf4j
@Service
@Transactional
public class AiMonitoringService {

    @Resource
    private AiMonitoringRepository monitoringRepository;

    @Value("${spring.ai.chat.model}")
    private String modelName;

    /**
     * 记录成功调用
     */
    public void recordSuccess(String conversationId, CallType callType, Integer userId, long duration) {
        AiMonitoringLogEntity entity = createMonitoringLog(conversationId, callType, userId, duration, true);
        monitoringRepository.save(entity);
        log.debug("[AI监控] 记录成功调用: {}", entity);
    }

    /**
     * 记录失败调用
     */
    public void recordFailure(String conversationId, CallType callType, Integer userId, long duration, String errorType, String errorMessage) {
        AiMonitoringLogEntity entity = createMonitoringLog(conversationId, callType, userId, duration, false);
        entity.setErrorType(errorType);
        entity.setErrorMessage(errorMessage);
        monitoringRepository.save(entity);
        log.debug("[AI监控] 记录失败调用: {}", entity);
    }

    /**
     * 获取调用历史记录
     */
    public List<AiMonitoringLogDto> getCallHistory(String conversationId, CallType callType, Integer userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        Page<AiMonitoringLogEntity> page;
        if (conversationId != null) {
            page = monitoringRepository.findByConversationIdAndUserIdOrderByStartTimeDesc(conversationId, userId, pageable);
        } else {
            // 使用统计查询替代自定义查询方法
            page = monitoringRepository.findAll(pageable);
        }

        return page.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取性能统计信息
     */
    public PerformanceStatisticsDto getStatistics(Integer userId, LocalDateTime startTime, LocalDateTime endTime, CallType callType) {
        // 使用现有的统计查询方法
        PerformanceStatisticsDto basicStats = monitoringRepository.getBasicStatistics(
                userId, startTime, endTime, modelName,
                callType != null ? callType.name() : null,
                "SUCCESS"
        );

        // 获取百分位数数据
        List<Object[]> percentileData = monitoringRepository.getPercentileLatency(
                userId, startTime, endTime, modelName,
                callType != null ? callType.name() : null,
                "SUCCESS"
        );

        double p50 = percentileData.isEmpty() ? 0 : (double) percentileData.get(0)[0];
        double p90 = percentileData.isEmpty() || percentileData.get(0).length < 2 ? 0 : (double) percentileData.get(0)[1];
        double p99 = percentileData.isEmpty() || percentileData.get(0).length < 3 ? 0 : (double) percentileData.get(0)[2];

        return PerformanceStatisticsDto.builder()
                .totalCalls(basicStats != null ? basicStats.getTotalCalls() : 0L)
                .successCalls(basicStats != null ? basicStats.getSuccessCalls() : 0L)
                .failureCalls(basicStats != null ? basicStats.getFailureCalls() : 0L)
                .successRate(basicStats != null ? basicStats.getSuccessRate() : 0.0)
                .avgLatencyMs(basicStats != null ? basicStats.getAvgLatencyMs() : 0.0)
                .maxLatencyMs(basicStats != null ? basicStats.getMaxLatencyMs() : 0L)
                .p90LatencyMs(p90)
                .p99LatencyMs(p99)
                .p50LatencyMs(p50)
                .minLatencyMs(basicStats != null ? basicStats.getMinLatencyMs() : 0L)
                .totalPromptTokens(basicStats != null ? basicStats.getTotalPromptTokens() : 0L)
                .totalCompletionTokens(basicStats != null ? basicStats.getTotalCompletionTokens() : 0L)
                .totalTokens(basicStats != null ? basicStats.getTotalTokens() : 0L)
                .errorBreakdown(List.of())
                .build();
    }

    /**
     * 估算P95延迟（基于P50/P90/P99进行插值）
     */
    private double p95Duration(double p50, double p90, double p99) {
        if (p99 == 0) return p90;
        if (p90 == 0) return p50;
        // 线性插值估算P95
        return p90 + (p99 - p90) * 0.5;
    }

    /**
     * 获取错误分析
     */
    public List<ErrorAnalysisDto> getErrorAnalysis(Integer userId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> errorData = monitoringRepository.getErrorAnalysis(userId, startTime, endTime);

        return errorData.stream()
                .map(data -> new ErrorAnalysisDto(
                        (String) data[0],      // errorType
                        (Long) data[1],       // count
                        0.0,                  // percentage - 计算逻辑可后续添加
                        (String) data[2]       // exampleMessage
                ))
                .toList();
    }

    /**
     * 获取趋势数据（临时实现）
     */
    public List<TrendDataDto> getTrendData(CallType callType, LocalDateTime startTime, LocalDateTime endTime) {
        // 返回空列表，待实现按小时聚合查询
        return List.of();
    }

    /**
     * 转换为DTO
     */
    private AiMonitoringLogDto convertToDto(AiMonitoringLogEntity entity) {
        return new AiMonitoringLogDto(
                entity.getId(),
                entity.getConversationId(),
                entity.getModelName(),
                entity.getCallType(),
                entity.getStartTime(),
                entity.getLatencyMs(),
                entity.getPromptTokens(),
                entity.getCompletionTokens(),
                entity.getTotalTokens(),
                entity.getStatus(),
                entity.getErrorType(),
                entity.getErrorMessage(),
                entity.getUserMessagePreview(),
                entity.getAssistantMessagePreview()
        );
    }

    /**
     * 创建监控日志实体
     */
    private AiMonitoringLogEntity createMonitoringLog(String conversationId, CallType callType, Integer userId, long duration, boolean success) {
        return AiMonitoringLogEntity.builder()
                .conversationId(conversationId)
                .modelName(modelName)
                .callType(callType.name())
                .userId(userId)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .latencyMs(duration)
                .promptTokens(0) // 暂时设为0，后续可扩展token统计
                .completionTokens(0)
                .totalTokens(0)
                .status(success ? "SUCCESS" : "FAILED")
                .userMessagePreview("") // 可后续扩展
                .assistantMessagePreview("") // 可后续扩展
                .build();
    }
}