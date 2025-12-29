package com.github.zavier.ai.validator;

import com.github.zavier.ai.IntentValidationService;
import com.github.zavier.ai.RateLimitService;
import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 聊天请求验证器
 * 使用责任链模式统一处理各种验证逻辑
 */
@Slf4j
@Component
public class ChatRequestValidator {

    @Resource
    private RateLimitService rateLimitService;

    @Resource
    private IntentValidationService intentValidationService;

    /**
     * 验证聊天请求
     *
     * @param request 聊天请求
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 验证结果，如果验证失败则包含拒绝响应
     */
    public ValidationResult validate(AiChatRequest request, String conversationId, Integer userId) {
        // 1. 速率限制验证
        ValidationResult rateLimitResult = checkRateLimit(conversationId, userId);
        if (rateLimitResult.isRejected()) {
            return rateLimitResult;
        }

        // 2. 意图验证
        ValidationResult intentResult = checkIntent(request, conversationId);
        if (intentResult.isRejected()) {
            return intentResult;
        }

        return ValidationResult.approved();
    }

    /**
     * 检查速率限制
     */
    private ValidationResult checkRateLimit(String conversationId, Integer userId) {
        if (!rateLimitService.allowRequest(userId)) {
            log.warn("[请求验证] 速率限制触发, conversationId={}, userId={}", conversationId, userId);

            long secondsUntilReset = rateLimitService.getSecondsUntilReset(userId);
            String message = buildRateLimitExceededMessage(secondsUntilReset);

            return ValidationResult.rejected(message);
        }

        log.debug("[请求验证] 速率限制检查通过, userId={}", userId);
        return ValidationResult.approved();
    }

    /**
     * 检查用户意图
     */
    private ValidationResult checkIntent(AiChatRequest request, String conversationId) {
        if (!intentValidationService.isExpenseRelated(request.message())) {
            log.info("[请求验证] 意图验证失败, conversationId={}, message={}",
                conversationId, request.message());

            String rejectionMessage = intentValidationService.getRejectionMessage();
            return ValidationResult.rejected(rejectionMessage, true); // 需要保存拒绝消息
        }

        log.debug("[请求验证] 意图验证通过");
        return ValidationResult.approved();
    }

    /**
     * 构建速率限制超出消息
     */
    private String buildRateLimitExceededMessage(long secondsUntilReset) {
        if (secondsUntilReset < 60) {
            return String.format("请求过于频繁，请在 %d 秒后重试。", secondsUntilReset);
        } else {
            long minutes = secondsUntilReset / 60;
            return String.format("请求过于频繁，请在 %d 分钟后重试。", minutes);
        }
    }

    /**
     * 验证结果
     */
    public record ValidationResult(
        boolean isRejected,
        String rejectionMessage,
        boolean saveRejection
    ) {
        /**
         * 是否被拒绝（为了可读性）
         */
        public boolean rejected() {
            return isRejected;
        }

        /**
         * 创建通过的结果
         */
        public static ValidationResult approved() {
            return new ValidationResult(false, null, false);
        }

        /**
         * 创建拒绝的结果
         */
        public static ValidationResult rejected(String message) {
            return new ValidationResult(true, message, false);
        }

        /**
         * 创建拒绝的结果（并保存拒绝消息）
         */
        public static ValidationResult rejected(String message, boolean saveRejection) {
            return new ValidationResult(true, message, saveRejection);
        }

        /**
         * 转换为响应对象
         */
        public AiChatResponse toResponse(String conversationId) {
            return AiChatResponse.builder()
                .conversationId(conversationId)
                .reply(rejectionMessage)
                .build();
        }
    }
}
