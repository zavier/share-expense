package com.github.zavier.dto.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseRecordSharingDTO {
    /**
     * 用户ID
     */
    private Integer userId;
    /**
     * 用户名称
     */
    private String userName;

    /**
     * 均摊权重
     */
    private Integer weight;

    /**
     * 均摊金额
     */
    private BigDecimal amount;
}
