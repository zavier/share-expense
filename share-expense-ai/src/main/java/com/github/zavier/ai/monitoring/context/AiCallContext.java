package com.github.zavier.ai.monitoring.context;

import com.github.zavier.web.filter.UserHolder;

/**
 * AI调用上下文容器（ThreadLocal）
 * 用于在业务层和Advisor之间传递调用信息
 */
public class AiCallContext {

    private static final ThreadLocal<CallInfo> CONTEXT = new ThreadLocal<>();

    /**
     * 设置调用上下文（自动获取当前用户）
     */
    public static void setContext(String conversationId, CallType callType) {
        Integer userId = getCurrentUserId();
        CONTEXT.set(new CallInfo(conversationId, callType, userId));
    }

    /**
     * 获取调用上下文
     */
    public static CallInfo get() {
        return CONTEXT.get();
    }

    /**
     * 清理调用上下文（必须调用，避免内存泄漏）
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前登录用户ID
     */
    private static Integer getCurrentUserId() {
        if (UserHolder.getUser() == null) {
            return null;
        }
        return UserHolder.getUser().getUserId();
    }

    /**
     * 调用信息记录
     */
    public record CallInfo(
            String conversationId,
            CallType callType,
            Integer userId
    ) {}

    /**
     * 调用类型枚举
     */
    public enum CallType {
        CHAT,       // 普通聊天
        SUGGESTION  // 建议生成
    }
}