package com.github.zavier.dto.data;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ExpenseRecordDTO {

    private Integer id;
    private Integer userId;
    private String userName;
    private Integer expenseProjectId;
    private BigDecimal amount;
    private Date date;
    private String expenseType;
    private String remark;
}
