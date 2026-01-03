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