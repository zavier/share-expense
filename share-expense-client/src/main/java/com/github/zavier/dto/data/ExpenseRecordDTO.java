package com.github.zavier.dto.data;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseRecordDTO {

    private Integer expenseProjectId;
    private Integer recordId;
    private String payMember;
    private BigDecimal amount;
    // 秒 时间戳
    private Long date;
    private String expenseType;
    private String remark;

    private List<String> consumeMembers = new ArrayList<>();
}
