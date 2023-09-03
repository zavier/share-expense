package com.github.zavier.domain.expense;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseSharing {
    /**
     * 费用均摊记录ID
     */
    private Integer expenseSharingId;

    /**
     * 费用记录ID
     */
    private Integer expenseRecordId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 均摊权重
     */
    private BigDecimal weight;

    /**
     * 均摊金额
     */
    private BigDecimal amount;
}




