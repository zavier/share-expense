package com.github.zavier.domain.user.domainservice;

/**
 * 密码编码器接口
 * <p>
 * 领域层只依赖此接口，具体实现在基础设施层。
 */
public interface PasswordEncoder {

    /**
     * 对原始密码进行编码
     *
     * @param rawPassword 原始密码
     * @return 编码后的密码哈希
     */
    String encode(String rawPassword);

    /**
     * 校验原始密码是否与编码后的密码匹配
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 编码后的密码哈希
     * @return 是否匹配
     */
    boolean matches(String rawPassword, String encodedPassword);
}
