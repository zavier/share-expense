package com.github.zavier.domain.user;

import lombok.Data;
import org.mindrot.jbcrypt.BCrypt;

@Data
public class User {
    /** 
     * 用户ID 
     */ 
    private int userId; 
     
    /** 
     * 用户名 
     */ 
    private String username; 
     
    /** 
     * 电子邮件 
     */ 
    private String email; 
     
    /** 
     * 密码哈希 
     */ 
    private String passwordHash;


    public String generatePasswordHash(String pwd) {
        // 生成盐值
        String salt = BCrypt.gensalt();
        // 使用盐值和密码生成哈希值
        return BCrypt.hashpw(pwd, salt);
    }

    public boolean checkPassword(String pwd) {
        return BCrypt.checkpw(pwd, this.passwordHash);
    }

}