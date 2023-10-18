package com.github.zavier.domain.expense;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ExpenseProjectMember {
    /**
     * 所属费用项目ID
     */
    private Integer projectId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 用户姓名
     */
    private String userName;


    /**
     * 均摊人份
     */
    private Integer weight;
}