package com.github.zavier.ai.monitoring.service;

import com.github.zavier.ai.monitoring.dto.AiMonitoringLogDto;
import com.github.zavier.ai.monitoring.dto.ErrorAnalysisDto;
import com.github.zavier.ai.monitoring.entity.AiMonitoringLogEntity;
import com.github.zavier.ai.monitoring.repository.AiMonitoringRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI调用监控服务
 * 提供AI调用的记录、统计和分析功能
 */
@Slf4j
@Service
public class AiMonitoringService {

    @Resource
    private AiMonitoringRepository monitoringRepository;

    public void record(AiMonitoringLogEntity entity) {
        log.info("[AI监控] 记录调用, entity={}", entity);
        monitoringRepository.saveAndFlush(entity);
    }
    /**
     * 获取调用历史记录
     */
    public List<AiMonitoringLogDto> getCallHistory(String conversationId, Integer userId, Pageable pageable) {
        Page<AiMonitoringLogEntity> page;
        if (conversationId != null && !conversationId.isEmpty()) {
            page = monitoringRepository.findByConversationIdAndUserIdOrderByStartTimeDesc(conversationId, userId, pageable);
        } else {
            // 使用用户隔离查询确保安全性
            page = monitoringRepository.findByUserIdOrderByStartTimeDesc(userId, pageable);
        }

        return page.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 转换为DTO
     */
    private AiMonitoringLogDto convertToDto(AiMonitoringLogEntity entity) {
        return new AiMonitoringLogDto(
                entity.getId(),
                entity.getConversationId(),
                entity.getModelName(),
                entity.getStartTime(),
                entity.getLatencyMs(),
                entity.getPromptTokens(),
                entity.getCompletionTokens(),
                entity.getTotalTokens(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getUserMessagePreview(),
                entity.getAssistantMessagePreview()
        );
    }

}