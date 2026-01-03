package com.github.zavier.ai.exception;

/**
 * AI 模块认证异常
 * 用于处理用户未认证或认证失败的情况
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
