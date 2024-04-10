package com.github.zavier.user;

import io.mybatis.provider.Entity;
import lombok.Data;

import java.util.Date;

@Data
@Entity.Table(value = "user", remark = "用户信息表", autoResultMap = true)
public class UserDO {
    @Entity.Column(value = "id", remark = "用户ID", id = true)
    private Integer id;

    @Entity.Column(value = "user_name", remark = "用户名")
    private String userName;

    @Entity.Column(value = "email", remark = "电子邮件")
    private String email;

    @Entity.Column(value = "password_hash", remark = "密码哈希")
    private String passwordHash;

    @Entity.Column(value = "created_at", remark = "创建时间")
    private Date createdAt;

    @Entity.Column(value = "updated_at", remark = "更新时间")
    private Date updatedAt;
}