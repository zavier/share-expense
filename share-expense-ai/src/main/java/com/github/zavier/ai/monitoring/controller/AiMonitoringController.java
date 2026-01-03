package com.github.zavier.ai.monitoring.controller;

import com.github.zavier.ai.exception.AuthenticationException;
import com.github.zavier.ai.monitoring.dto.AiMonitoringLogDto;
import com.github.zavier.ai.monitoring.dto.ErrorAnalysisDto;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Integer userId = getCurrentUserId();

        List<AiMonitoringLogDto> history = monitoringService.getCallHistory(
            conversationId, userId, pageable
        );

        return SingleResponseVo.of(history);
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