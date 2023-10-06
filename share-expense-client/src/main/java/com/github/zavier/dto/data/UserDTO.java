package com.github.zavier.dto.data;

import lombok.Data;

@Data
public class UserDTO {
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
}
