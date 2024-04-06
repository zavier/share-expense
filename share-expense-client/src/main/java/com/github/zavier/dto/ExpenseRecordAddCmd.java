package com.github.zavier.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ExpenseRecordAddCmd {
    private Integer projectId;
    private BigDecimal amount;
    private Date date;
    private String expenseType;
    private String remark;

    /**
     * 消费用户ID集合，逗号分隔
     */
    private String consumerIds;

    /**
     * 付款用户ID
     */
    private Integer payUserId;

    public List<Integer> listConsumerIds() {
        if (consumerIds == null || consumerIds.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(consumerIds.trim().split(","))
                .map(Integer::parseInt).collect(Collectors.toList());
    }
}
