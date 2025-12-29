package com.github.zavier.ai.domain;

/**
 * 消息角色枚举
 * 定义对话中消息的发送者角色
 */
public enum MessageRole {
    /**
     * 用户消息
     */
    USER("user"),

    /**
     * AI助手消息
     */
    ASSISTANT("assistant"),

    /**
     * 系统消息
     */
    SYSTEM("system");

    private final String code;

    MessageRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据代码获取枚举
     */
    public static MessageRole fromCode(String code) {
        for (MessageRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown message role code: " + code);
    }
}
