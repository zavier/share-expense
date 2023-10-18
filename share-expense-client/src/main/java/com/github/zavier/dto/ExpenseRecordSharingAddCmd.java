package com.github.zavier.dto;

import lombok.Data;

@Data
public class ExpenseRecordSharingAddCmd {
    private Integer recordId;
    private Integer userId;
    private Integer weight;
}
