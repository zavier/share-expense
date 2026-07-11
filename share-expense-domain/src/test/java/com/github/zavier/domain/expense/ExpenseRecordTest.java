package com.github.zavier.domain.expense;

import com.alibaba.cola.exception.BizException;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ExpenseRecordTest {

    @Test
    public void calcMembersFeeInRecord_payerIsConsumer_shouldSplitCorrectly() {
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

    @Test
    public void calcMembersFeeInRecord_payerIsAlsoConsumer_shouldNotDuplicate() {
        ExpenseRecord record = new ExpenseRecord();
        record.addConsumer("Alice");
        record.addConsumer("Bob");
        record.setPayMember("Alice");
        record.setAmount(new BigDecimal(100));

        List<MemberRecordFee> fees = record.calcMembersFeeInRecord();

        // Alice is both payer and consumer → only 2 entries, not 3
        assertEquals(2, fees.size());
        for (MemberRecordFee fee : fees) {
            if ("Alice".equals(fee.getMember())) {
                assertEquals(0, new BigDecimal(50).compareTo(fee.getConsumeAmount()));
                assertEquals(0, new BigDecimal(100).compareTo(fee.getPaidAmount()));
            } else if ("Bob".equals(fee.getMember())) {
                assertEquals(0, new BigDecimal(50).compareTo(fee.getConsumeAmount()));
                assertEquals(0, BigDecimal.ZERO.compareTo(fee.getPaidAmount()));
            }
        }
    }

    @Test
    public void calcMembersFeeInRecord_singleConsumer_payerNotConsumer_shouldHaveTwoEntries() {
        ExpenseRecord record = new ExpenseRecord();
        record.addConsumer("Bob");
        record.setPayMember("Alice");
        record.setAmount(new BigDecimal(50));

        List<MemberRecordFee> fees = record.calcMembersFeeInRecord();

        // Bob (consumer) + Alice (payer who didn't consume)
        assertEquals(2, fees.size());
    }

    @Test
    public void calcMembersFeeInRecord_unevenAmount_shouldUseHighPrecision() {
        ExpenseRecord record = new ExpenseRecord();
        record.addConsumer("Alice");
        record.addConsumer("Bob");
        record.addConsumer("Charlie");
        record.setPayMember("Alice");
        record.setAmount(new BigDecimal("100"));

        List<MemberRecordFee> fees = record.calcMembersFeeInRecord();

        // 100/3 = 33.333333 (6 decimal places)
        for (MemberRecordFee fee : fees) {
            if (!"Alice".equals(fee.getMember())) {
                // Should have high precision division
                assertTrue(fee.getConsumeAmount().compareTo(new BigDecimal("33.33")) > 0);
                assertTrue(fee.getConsumeAmount().compareTo(new BigDecimal("33.34")) < 0);
            }
        }
    }

    // ==================== addConsumer ====================

    @Test
    public void addConsumer_valid_shouldSucceed() {
        ExpenseRecord record = new ExpenseRecord();
        record.addConsumer("Alice");

        Set<String> consumers = record.listAllConsumers();
        assertEquals(1, consumers.size());
        assertTrue(consumers.contains("Alice"));
    }

    @Test(expected = BizException.class)
    public void addConsumer_duplicate_shouldThrow() {
        ExpenseRecord record = new ExpenseRecord();
        record.addConsumer("Alice");
        record.addConsumer("Alice");
    }

    @Test
    public void addConsumers_list_shouldAddAll() {
        ExpenseRecord record = new ExpenseRecord();
        record.addConsumers(Arrays.asList("Alice", "Bob", "Charlie"));

        assertEquals(3, record.listAllConsumers().size());
    }

    @Test
    public void listAllConsumers_shouldReturnUnmodifiable() {
        ExpenseRecord record = new ExpenseRecord();
        record.addConsumer("Alice");

        Set<String> consumers = record.listAllConsumers();
        try {
            consumers.add("Bob");
            fail();
        } catch (UnsupportedOperationException e) {
            // expected — unmodifiable set
        }
    }

    // ==================== updateInfo ====================

    @Test
    public void updateInfo_allFieldsChanged_shouldReturnTrue() {
        ExpenseRecord original = createRecord(1, 1, "Alice", 100, "餐饮");
        original.addConsumer("Alice");
        original.addConsumer("Bob");

        ExpenseRecord update = createRecord(1, 1, "Bob", 200, "交通");
        update.addConsumer("Charlie");

        boolean changed = original.updateInfo(update);
        assertTrue(changed);
        assertEquals("Bob", original.getPayMember());
        assertEquals(0, new BigDecimal(200).compareTo(original.getAmount()));
        assertEquals("交通", original.getExpenseType());
        assertTrue(original.listAllConsumers().contains("Charlie"));
    }

    @Test
    public void updateInfo_noChanges_shouldReturnFalse() {
        ExpenseRecord original = createRecord(1, 1, "Alice", 100, "餐饮");
        original.addConsumer("Alice");
        original.addConsumer("Bob");

        ExpenseRecord update = createRecord(1, 1, "Alice", 100, "餐饮");
        update.addConsumer("Alice");
        update.addConsumer("Bob");

        boolean changed = original.updateInfo(update);
        assertFalse(changed);
    }

    @Test(expected = BizException.class)
    public void updateInfo_differentProject_shouldThrow() {
        ExpenseRecord original = createRecord(1, 1, "Alice", 100, "餐饮");
        ExpenseRecord update = createRecord(1, 2, "Alice", 100, "餐饮");

        original.updateInfo(update);
    }

    @Test(expected = BizException.class)
    public void updateInfo_differentId_shouldThrow() {
        ExpenseRecord original = createRecord(1, 1, "Alice", 100, "餐饮");
        ExpenseRecord update = createRecord(2, 1, "Alice", 100, "餐饮");

        original.updateInfo(update);
    }

    // ==================== helpers ====================

    private ExpenseRecord createRecord(int id, int projectId, String payer, int amount, String expenseType) {
        ExpenseRecord record = new ExpenseRecord();
        record.setId(id);
        record.setProjectId(projectId);
        record.setPayMember(payer);
        record.setAmount(new BigDecimal(amount));
        record.setDate(new Date());
        record.setExpenseType(expenseType);
        record.setRemark("test remark");
        return record;
    }
}