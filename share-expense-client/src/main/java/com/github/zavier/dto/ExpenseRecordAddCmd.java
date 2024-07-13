package com.github.zavier.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseRecordAddCmd {
    private Integer projectId;
    private BigDecimal amount;
    // 秒 时间戳
    private Long date;
    private String expenseType;
    private String remark;

    /**
     * 消费用户集合
     */
    private List<String> consumerMembers = new ArrayList<>();

    /**
     * 付款用户ID
     */
    private String payMember;

    private Integer operatorId;

}
