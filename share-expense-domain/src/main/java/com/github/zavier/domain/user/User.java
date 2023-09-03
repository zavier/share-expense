package com.github.zavier.domain.user;

import java.util.Date;

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
     
    /** 
     * 创建时间 
     */ 
    private Date createdAt;
     
    /** 
     * 更新时间 
     */ 
    private Date updatedAt; 
}