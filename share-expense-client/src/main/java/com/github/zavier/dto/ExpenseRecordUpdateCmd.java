package com.github.zavier.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExpenseRecordUpdateCmd extends ExpenseRecordAddCmd {
    private Integer recordId;
}
