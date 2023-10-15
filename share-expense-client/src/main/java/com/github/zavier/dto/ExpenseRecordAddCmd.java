package com.github.zavier.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ExpenseRecordAddCmd {
    private Integer userId;
    private Integer projectId;
    private BigDecimal amount;
    private Date date;
    private String expenseType;
    private String remark;
}
