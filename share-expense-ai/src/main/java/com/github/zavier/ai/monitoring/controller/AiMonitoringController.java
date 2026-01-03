package com.github.zavier.ai.monitoring.controller;

import com.github.zavier.ai.monitoring.context.AiCallContext;
import com.github.zavier.ai.monitoring.context.AiCallContext.CallType;
import com.github.zavier.ai.monitoring.dto.AiMonitoringLogDto;
import com.github.zavier.ai.monitoring.dto.ErrorAnalysisDto;
import com.github.zavier.ai.monitoring.dto.PerformanceStatisticsDto;
import com.github.zavier.ai.monitoring.dto.TrendDataDto;
import com.github.zavier.ai.monitoring.service.AiMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI调用监控控制器
 * 提供监控数据的REST接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/monitoring")
public class AiMonitoringController {

    private final AiMonitoringService monitoringService;

    public AiMonitoringController(AiMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * 获取调用历史记录
     */
    @GetMapping("/history")
    public ResponseEntity<List<AiMonitoringLogDto>> getCallHistory(
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String callType,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        CallType type = callType != null ? CallType.valueOf(callType) : null;

        List<AiMonitoringLogDto> history = monitoringService.getCallHistory(
            conversationId, type, userId, startTime, endTime, pageable
        );

        return ResponseEntity.ok(history);
    }

    /**
     * 获取性能统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<PerformanceStatisticsDto> getStatistics(
            @RequestParam(required = false) String callType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        CallType type = callType != null ? CallType.valueOf(callType) : null;
        Integer userId = getCurrentUserId(); // 从上下文获取当前用户

        PerformanceStatisticsDto statistics = monitoringService.getStatistics(
            userId, startTime, endTime, type
        );

        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取错误分析
     */
    @GetMapping("/errors")
    public ResponseEntity<List<ErrorAnalysisDto>> getErrorAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        Integer userId = getCurrentUserId(); // 从上下文获取当前用户

        List<ErrorAnalysisDto> errorAnalysis = monitoringService.getErrorAnalysis(
            userId, startTime, endTime
        );

        return ResponseEntity.ok(errorAnalysis);
    }

    /**
     * 获取趋势数据
     */
    @GetMapping("/trends")
    public ResponseEntity<List<TrendDataDto>> getTrends(
            @RequestParam(required = false) String callType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        CallType type = callType != null ? CallType.valueOf(callType) : null;

        List<TrendDataDto> trends = monitoringService.getTrendData(type, startTime, endTime);

        return ResponseEntity.ok(trends);
    }

    /**
     * 获取当前用户ID（从安全上下文）
     */
    private Integer getCurrentUserId() {
        try {
            return AiCallContext.get().userId();
        } catch (Exception e) {
            log.warn("无法获取当前用户ID，使用null", e);
            return null;
        }
    }
}