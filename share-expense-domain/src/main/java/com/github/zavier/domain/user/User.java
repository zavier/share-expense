package com.github.zavier.domain.user;

import com.github.zavier.domain.user.domainservice.PasswordEncoder;
import com.github.zavier.domain.user.domainservice.TokenProvider;
import lombok.Data;

import java.util.UUID;

@Data
public class User {
    /**
     * 用户ID
     */
    private int userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 电子邮件
     */
    private String email;

    /**
     * 密码哈希
     */
    private String passwordHash;

    /**
     * 微信openId
     */
    private String openId;


    public boolean checkPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawPassword, this.passwordHash);
    }

    public String generateToken(TokenProvider tokenProvider) {
        return tokenProvider.generateToken(this);
    }

    public String generateWxUserName() {
        final String substring = UUID.randomUUID().toString().replace("-", "").substring(6, 24);
        return "wx_" + substring;
    }
}