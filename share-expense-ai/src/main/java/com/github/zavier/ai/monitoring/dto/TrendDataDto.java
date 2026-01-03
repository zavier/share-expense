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