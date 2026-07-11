package com.github.zavier.domain.user.domainservice;

import com.github.zavier.domain.user.User;

/**
 * Token 提供者接口
 * <p>
 * 领域层只依赖此接口，具体实现在基础设施层。
 */
public interface TokenProvider {

    /**
     * 为用户生成 JWT Token
     *
     * @param user 用户
     * @return JWT Token 字符串
     */
    String generateToken(User user);

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    boolean verifyToken(String token);

    /**
     * 从 Token 中解析用户信息
     *
     * @param token JWT Token
     * @return 用户对象
     */
    User getUser(String token);
}
