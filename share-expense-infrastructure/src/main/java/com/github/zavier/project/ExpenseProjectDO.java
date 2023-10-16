package com.github.zavier.project;

import io.mybatis.provider.Entity;
import lombok.Data;

import java.util.Date;

@Data
@Entity.Table(value = "expense_project", remark = "费用项目信息", autoResultMap = true)
public class ExpenseProjectDO {
    @Entity.Column(value = "id", remark = "费用项目ID", id = true)
    private Integer id;
    @Entity.Column(value = "name", remark = "项目名称")
    private String name;
    @Entity.Column(value = "create_user_id", remark = "创建者用户ID")
    private Integer createUserId;
    @Entity.Column(value = "description", remark = "项目描述")
    private String description;
    @Entity.Column(value = "version", remark = "版本号")
    private Integer version;
    @Entity.Column(value = "created_at", remark = "创建时间")
    private Date createdAt;
    @Entity.Column(value = "updated_at", remark = "更新时间")
    private Date updatedAt;
}
