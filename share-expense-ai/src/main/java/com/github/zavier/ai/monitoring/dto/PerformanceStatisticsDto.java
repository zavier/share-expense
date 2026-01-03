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