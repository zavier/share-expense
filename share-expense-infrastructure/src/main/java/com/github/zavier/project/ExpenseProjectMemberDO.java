package com.github.zavier.project;

import io.mybatis.provider.Entity;
import lombok.Data;

import java.util.Date;

@Data
@Entity.Table(value = "expense_project_member", remark = "费用项目成员关联信息", autoResultMap = true)
public class ExpenseProjectMemberDO {
    @Entity.Column(value = "id", remark = "费用项目成员ID")
    private Integer id;
    @Entity.Column(value = "project_id", remark = "所属费用项目ID")
    private Integer projectId;
    @Entity.Column(value = "name", remark = "用户名称")
    private String name;
    @Entity.Column(value = "created_at", remark = "创建时间")
    private Date createdAt;
    @Entity.Column(value = "updated_at", remark = "更新时间")
    private Date updatedAt;
}