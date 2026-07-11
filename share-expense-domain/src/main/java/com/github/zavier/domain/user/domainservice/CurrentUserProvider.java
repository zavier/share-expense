package com.github.zavier.domain.user.domainservice;

/**
 * 当前用户提供者接口
 * <p>
 * 领域层只依赖此接口，具体实现由 adapter 层提供（如通过 ThreadLocal 从 HTTP 请求上下文获取）。
 */
public interface CurrentUserProvider {

    /**
     * 获取当前登录用户 ID
     *
     * @return 当前用户 ID
     * @throws RuntimeException 如果用户未登录（未认证）
     */
    Integer getCurrentUserId();
}
