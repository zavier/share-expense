package com.github.zavier.domain.expense;

import lombok.Data;

import java.util.Date;

@Data
public class ExpenseProject {
    /**
     * 费用项目ID
     */
    private Integer expenseProjectId;

    /**
     * 创建者用户ID
     */
    private Integer userId;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}