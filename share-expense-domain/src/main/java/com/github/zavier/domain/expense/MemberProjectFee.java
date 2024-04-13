package com.github.zavier.domain.expense;

import com.alibaba.cola.exception.Assert;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 单个成员在一个项目中的费用信息
 */
public class MemberProjectFee {

    @Getter
    private String member;

    /**
     * 费用记录的总金额
     */
    @Getter
    private BigDecimal recordAmount = BigDecimal.ZERO;

    /**
     * 本人支出金额
     */
    @Getter
    private BigDecimal paidAmount = BigDecimal.ZERO;
    /**
     * 本人消费金额
     */
    @Getter
    private BigDecimal consumeAmount = BigDecimal.ZERO;

    /**
     * 费用明细
     */
    private List<MemberRecordFee> memberFeeDetailList = new ArrayList<>();


    public void addFeeDetail(MemberRecordFee detail) {
        if (member == null) {
            member = detail.getMember();
        } else {
            Assert.isTrue(Objects.equals(member, detail.getMember()), "非当前成员费用");
        }

        recordAmount = recordAmount.add(detail.getExpenseRecord().getAmount());
        paidAmount = paidAmount.add(detail.getPaidAmount());
        consumeAmount = consumeAmount.add(detail.getConsumeAmount());

        memberFeeDetailList.add(detail);
    }

    public List<MemberRecordFee> getMemberFeeDetailList() {
        return Collections.unmodifiableList(memberFeeDetailList);
    }
}
