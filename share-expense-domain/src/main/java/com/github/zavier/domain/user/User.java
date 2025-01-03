package com.github.zavier.domain.user;

import com.github.zavier.domain.utils.PasswordEncoder;
import com.github.zavier.domain.utils.TokenHelper;
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


    public String generatePasswordHash(String pwd) {
        return PasswordEncoder.encode(pwd);
    }

    public boolean checkPassword(String pwd) {
        return PasswordEncoder.checkPassword(pwd, this.passwordHash);
    }

    public String generateToken() {
        return TokenHelper.generateToken(this);
    }

    public String generateWxUserName() {
        final String substring = UUID.randomUUID().toString().replace("-", "").substring(6, 24);
        return "wx_" + substring;
    }
}