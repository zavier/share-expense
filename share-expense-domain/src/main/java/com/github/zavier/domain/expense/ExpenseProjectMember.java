package com.github.zavier.domain.expense;

import lombok.Data;

@Data
public class ExpenseProjectMember {
    /**
     * 费用项目成员ID
     */
    private Integer expenseProjectMemberId;

    /**
     * 所属费用项目ID
     */
    private Integer expenseProjectId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 是否接受邀请
     */
    private Boolean isAccepted;
}