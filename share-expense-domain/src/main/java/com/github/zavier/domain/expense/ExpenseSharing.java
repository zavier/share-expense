package com.github.zavier.domain.expense;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ExpenseSharing {

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 均摊权重
     */
    private Integer weight;

    /**
     * 均摊金额
     */
    private BigDecimal amount;

    public ExpenseSharing() {}

    public ExpenseSharing(Integer userId, Integer weight) {
        this.userId = userId;
        this.weight = weight;
    }

}




