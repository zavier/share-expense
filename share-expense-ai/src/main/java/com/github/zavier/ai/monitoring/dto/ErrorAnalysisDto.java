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