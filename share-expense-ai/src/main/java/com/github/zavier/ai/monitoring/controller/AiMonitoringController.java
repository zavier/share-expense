package com.github.zavier.ai.monitoring.controller;

import com.github.zavier.ai.exception.AuthenticationException;
import com.github.zavier.ai.monitoring.context.AiCallContext.CallType;
import com.github.zavier.ai.monitoring.dto.AiMonitoringLogDto;
import com.github.zavier.ai.monitoring.dto.ErrorAnalysisDto;
import com.github.zavier.ai.monitoring.dto.PerformanceStatisticsDto;
import com.github.zavier.ai.monitoring.dto.TrendDataDto;
import com.github.zavier.ai.monitoring.service.AiMonitoringService;
import com.github.zavier.web.filter.UserHolder;
import com.github.zavier.vo.SingleResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
    @GetMapping("/session/{conversationId}/history")
    public SingleResponseVo<List<AiMonitoringLogDto>> getCallHistory(
            @PathVariable String conversationId,
            @RequestParam(required = false) String callType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        CallType type = callType != null ? CallType.valueOf(callType) : null;
        Integer userId = getCurrentUserId();

        List<AiMonitoringLogDto> history = monitoringService.getCallHistory(
            conversationId, type, userId, startTime, endTime, pageable
        );

        return SingleResponseVo.of(history);
    }

    /**
     * 获取性能统计信息（最近7天概览）
     */
    @GetMapping("/overview")
    public SingleResponseVo<PerformanceStatisticsDto> getOverview(
            @RequestParam(required = false) String callType) {

        CallType type = callType != null ? CallType.valueOf(callType) : null;
        Integer userId = getCurrentUserId();

        // 默认查询最近7天
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(7);

        PerformanceStatisticsDto statistics = monitoringService.getStatistics(
            userId, startTime, endTime, type
        );

        return SingleResponseVo.of(statistics);
    }

    /**
     * 获取性能统计信息
     */
    @GetMapping("/statistics")
    public SingleResponseVo<PerformanceStatisticsDto> getStatistics(
            @RequestParam(required = false) String callType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        CallType type = callType != null ? CallType.valueOf(callType) : null;
        Integer userId = getCurrentUserId();

        PerformanceStatisticsDto statistics = monitoringService.getStatistics(
            userId, startTime, endTime, type
        );

        return SingleResponseVo.of(statistics);
    }

    /**
     * 获取错误分析
     */
    @GetMapping("/errors/analysis")
    public SingleResponseVo<List<ErrorAnalysisDto>> getErrorAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        Integer userId = getCurrentUserId();

        List<ErrorAnalysisDto> errorAnalysis = monitoringService.getErrorAnalysis(
            userId, startTime, endTime
        );

        return SingleResponseVo.of(errorAnalysis);
    }

    /**
     * 获取趋势数据
     */
    @GetMapping("/trends")
    public SingleResponseVo<List<TrendDataDto>> getTrends(
            @RequestParam(required = false) String callType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        CallType type = callType != null ? CallType.valueOf(callType) : null;

        List<TrendDataDto> trends = monitoringService.getTrendData(type, startTime, endTime);

        return SingleResponseVo.of(trends);
    }

    /**
     * 获取当前用户ID（从安全上下文）
     */
    private Integer getCurrentUserId() {
        if (UserHolder.getUser() == null) {
            throw new AuthenticationException("用户未登录");
        }
        return UserHolder.getUser().getUserId();
    }
}