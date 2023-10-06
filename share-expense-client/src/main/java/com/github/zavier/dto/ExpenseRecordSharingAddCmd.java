package com.github.zavier.dto;

import lombok.Data;

@Data
public class ExpenseRecordSharingAddCmd {
    private Integer expenseRecordId;
    private Integer userId;
    private Integer weight;
}
