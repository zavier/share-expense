package com.github.zavier.ai.monitoring.advisor;

import com.github.zavier.ai.monitoring.entity.AiMonitoringLogEntity;
import com.github.zavier.ai.monitoring.service.AiMonitoringService;
import com.github.zavier.web.filter.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * AI调用监控拦截器
 * 通过手动包装方式实现调用拦截和监控
 */
@Slf4j
@Component
public class AiMonitoringAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String CONVERSATION_ID_KEY = "conversationId";

    private final AiMonitoringService monitoringService;

    public AiMonitoringAdvisor(AiMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.info("[AI监控] adviseCall");
        final Object conversationId = chatClientRequest.context().get(CONVERSATION_ID_KEY);
        long startTime = System.currentTimeMillis();
        Optional<ChatResponseMetadata> metadataOptional = Optional.empty();
        Optional<Usage> usageOptional = Optional.empty();
        @Nullable ChatClientResponse chatClientResponse = null;
        try {
            // execute the call
            chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

            metadataOptional = Optional.ofNullable(chatClientResponse.chatResponse())
                    .map(ChatResponse::getMetadata);
            usageOptional = metadataOptional.map(ChatResponseMetadata::getUsage);

            // 记录成功调用
            final long endTime = System.currentTimeMillis();

            final AiMonitoringLogEntity monitoringLog = AiMonitoringLogEntity.builder()
                    .userId(UserHolder.getUser().getUserId())
                    .conversationId(conversationId == null ? "-1" : conversationId.toString())
                    .modelName(metadataOptional.map(ChatResponseMetadata::getModel).orElse(""))
                    .startTime(convertMillisecondToDateTimeUTC(startTime))
                    .endTime(convertMillisecondToDateTimeUTC(endTime))
                    .latencyMs(endTime - startTime)
                    .promptTokens(usageOptional.map(Usage::getPromptTokens).orElse(0))
                    .completionTokens(usageOptional.map(Usage::getCompletionTokens).orElse(0))
                    .totalTokens(usageOptional.map(Usage::getTotalTokens).orElse(0))
                    .status("SUCCESS")
                    .userMessagePreview(max500(chatClientRequest.prompt().getContents()))
                    .assistantMessagePreview(getResponseMax500Len(chatClientResponse))
                    .createdAt(LocalDateTime.now())
                    .build();
            monitoringService.record(monitoringLog);

            return chatClientResponse;

        } catch (Exception e) {
            log.error("[AI监控] 调用失败", e);
            final long endTime = System.currentTimeMillis();
            final AiMonitoringLogEntity monitoringLog = AiMonitoringLogEntity.builder()
                    .userId(UserHolder.getUser().getUserId())
                    .conversationId(conversationId == null ? "-1" : conversationId.toString())
                    .modelName(metadataOptional.map(ChatResponseMetadata::getModel).orElse(""))
                    .startTime(convertMillisecondToDateTimeUTC(startTime))
                    .endTime(convertMillisecondToDateTimeUTC(endTime))
                    .latencyMs(endTime - startTime)
                    .promptTokens(usageOptional.map(Usage::getPromptTokens).orElse(0))
                    .completionTokens(usageOptional.map(Usage::getCompletionTokens).orElse(0))
                    .totalTokens(usageOptional.map(Usage::getTotalTokens).orElse(0))
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .userMessagePreview(max500(chatClientRequest.prompt().getContents()))
                    .assistantMessagePreview(getResponseMax500Len(chatClientResponse))
                    .createdAt(LocalDateTime.now())
                    .build();
            monitoringService.record(monitoringLog);

            throw e;
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        log.info("[AI监控] adviseStream");
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    private static String getResponseMax500Len(ChatClientResponse chatClientResponse) {
        if (chatClientResponse == null) {
            return "";
        }

        final ChatResponse chatResponse = chatClientResponse.chatResponse();
        if (chatResponse == null) {
            return "";
        }
        final Generation result = chatResponse.getResult();
        if (result == null || result.getOutput() == null) {
            return "";
        }

        return max500(result.getOutput().toString());
    }

    private static String max500(String input) {
        if (StringUtils.isBlank(input)) {
            return "";
        }
        return input.length() > 500 ? input.substring(0, 500) : input;
    }

    private static LocalDateTime convertMillisecondToDateTimeUTC(long millisecond) {
        // 将毫秒时间戳转换为 Instant
        Instant instant = Instant.ofEpochMilli(millisecond);
        // 使用 UTC 时区偏移量转换为 LocalDateTime
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

}