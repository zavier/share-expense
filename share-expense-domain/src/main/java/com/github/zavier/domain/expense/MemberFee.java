package com.github.zavier.domain.expense;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class MemberFee {

    private String member;

    private Integer recordId;

    /**
     * 费用记录的总金额
     */
    private BigDecimal recordAmount;

    /**
     * 本人支出金额
     */
    private BigDecimal paidAmount;
    /**
     * 本人消费金额
     */
    private BigDecimal consumeAmount;

}
