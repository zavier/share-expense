package com.github.zavier.ai;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.ai.exception.AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * AI 模块全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public SingleResponse<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("[AI模块] 认证失败: {}", e.getMessage());
        return SingleResponse.buildFailure("AUTH_ERROR", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public SingleResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("[AI模块] 参数校验失败: {}", e.getMessage());
        return SingleResponse.buildFailure("PARAM_ERROR", e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public SingleResponse<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("资源不存在 NoResourceFoundException: {}", e.getMessage());
        return SingleResponse.buildFailure("PARAM_ERROR", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public SingleResponse<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return SingleResponse.buildFailure("INTERNAL_ERROR", "系统错误");
    }
}
