package com.github.zavier.domain.expense;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ExpenseRecord {
    /**
     * 费用记录ID
     */
    private Integer expenseRecordId;
    
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 费用项目ID
     */
    private Integer expenseProjectId;
    
    /**
     * 费用来源
     */
    private String source;
    
    /**
     * 费用金额
     */
    private BigDecimal amount;
    
    /**
     * 费用日期
     */
    private Date date;
    
    /**
     * 是否已均摊
     */
    private Boolean isShared;
}