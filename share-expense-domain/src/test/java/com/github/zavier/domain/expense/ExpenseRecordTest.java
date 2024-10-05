package com.github.zavier.domain.expense;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExpenseRecordTest {



    @Test
    public void calcMembersFeeInRecord() {
        final ExpenseRecord expenseRecord = new ExpenseRecord();
        expenseRecord.addConsumer("u1");
        expenseRecord.addConsumer("u2");
        expenseRecord.addConsumer("u3");

        expenseRecord.setPayMember("p1");
        expenseRecord.setAmount(new BigDecimal(90));
        final List<MemberRecordFee> memberRecordFees = expenseRecord.calcMembersFeeInRecord();
        assertEquals(4, memberRecordFees.size());
        for (MemberRecordFee memberRecordFee : memberRecordFees) {
            if (!memberRecordFee.getMember().startsWith("p")) {
                assertEquals(0, new BigDecimal(30).compareTo(memberRecordFee.getConsumeAmount()));
                assertEquals(0, BigDecimal.ZERO.compareTo(memberRecordFee.getPaidAmount()));
            } else {
                assertEquals(0, BigDecimal.ZERO.compareTo(memberRecordFee.getConsumeAmount()));
                assertEquals(0, new BigDecimal(90).compareTo(memberRecordFee.getPaidAmount()));
            }
        }
    }
}