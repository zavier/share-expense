package com.github.zavier.project;

import io.mybatis.provider.Entity;
import lombok.Data;

@Data
@Entity.Table(value = "expense_project_member", remark = "费用项目成员关联信息", autoResultMap = true)
public class ExpenseProjectMemberDO {
    @Entity.Column(value = "id", remark = "费用项目成员ID")
    private Integer id;
    @Entity.Column(value = "expense_project_id", remark = "所属费用项目ID")
    private Integer expenseProjectId;
    @Entity.Column(value = "user_id", remark = "用户ID")
    private Integer userId;
}