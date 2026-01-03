package com.github.zavier.ai.monitoring.advisor;

import com.github.zavier.ai.monitoring.context.AiCallContext;
import com.github.zavier.ai.monitoring.context.AiCallContext.CallInfo;
import com.github.zavier.ai.monitoring.context.AiCallContext.CallType;
import com.github.zavier.ai.monitoring.service.AiMonitoringService;
import com.github.zavier.web.filter.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI调用监控拦截器
 * 通过手动包装方式实现调用拦截和监控
 */
@Slf4j
@Component
public class AiMonitoringAdvisor {

    private final AiMonitoringService monitoringService;

    public AiMonitoringAdvisor(AiMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * 拦截ChatClient调用
     */
    public Object monitorCall(CallType callType, Runnable execution) {
        AiCallContext.setContext(null, callType); // 会在具体业务方法中设置正确的conversationId
        CallInfo callInfo = AiCallContext.get();
        long startTime = System.currentTimeMillis();

        try {
            // 执行业务逻辑
            execution.run();

            // 记录成功调用
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.recordSuccess(
                callInfo != null ? callInfo.conversationId() : null,
                callInfo != null ? callInfo.callType() : null,
                callInfo != null ? callInfo.userId() : UserHolder.getUser().getUserId(),
                duration
            );

            log.debug("[AI监控] 调用成功, duration={}ms, callInfo={}", duration, callInfo);
            return null;

        } catch (Exception e) {
            // 记录失败调用
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.recordFailure(
                callInfo != null ? callInfo.conversationId() : null,
                callInfo != null ? callInfo.callType() : null,
                callInfo != null ? callInfo.userId() : UserHolder.getUser().getUserId(),
                duration,
                e.getClass().getSimpleName(),
                e.getMessage()
            );

            log.error("[AI监控] 调用失败, duration={}ms, callInfo={}", duration, callInfo, e);
            throw e; // 重新抛出异常
        } finally {
            // 清理ThreadLocal上下文
            AiCallContext.clear();
        }
    }
}