package com.github.zavier.domain.expense;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 单个费用记录，单个人的费用明细
 *
 */
@Data
@Accessors(chain = true)
public class MemberRecordFee {

    private String member;

    /**
     * 费用记录信息
     */
    private ExpenseRecord expenseRecord;

    /**
     * 本人支出金额
     */
    private BigDecimal paidAmount;
    /**
     * 本人消费金额
     */
    private BigDecimal consumeAmount;

}
