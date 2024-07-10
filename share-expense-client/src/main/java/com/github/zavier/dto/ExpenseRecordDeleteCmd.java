package com.github.zavier.dto;

import lombok.Data;

@Data
public class ExpenseRecordDeleteCmd {
    private Integer projectId;

    private Integer recordId;

    private Integer operatorId;

}
