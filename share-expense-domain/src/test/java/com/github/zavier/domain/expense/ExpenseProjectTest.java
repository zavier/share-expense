package com.github.zavier.domain.expense;

import com.alibaba.cola.exception.BizException;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ExpenseProject 聚合根测试
 */
public class ExpenseProjectTest {

    // ==================== addMember ====================

    @Test
    public void addMember_validMember_shouldSucceed() {
        ExpenseProject project = new ExpenseProject();
        project.addMember("Alice");

        assertTrue(project.containsMember("Alice"));
        assertEquals(1, project.totalMember());
    }

    @Test(expected = BizException.class)
    public void addMember_blankName_shouldThrow() {
        ExpenseProject project = new ExpenseProject();
        project.addMember("");
    }

    @Test(expected = BizException.class)
    public void addMember_duplicateName_shouldThrow() {
        ExpenseProject project = new ExpenseProject();
        project.addMember("Alice");
        project.addMember("Alice");
    }

    @Test
    public void addMembers_nullList_shouldNotFail() {
        ExpenseProject project = new ExpenseProject();
        project.addMembers(null);
        assertEquals(0, project.totalMember());
    }

    @Test
    public void addMembers_validList_shouldAddAll() {
        ExpenseProject project = new ExpenseProject();
        project.addMembers(Arrays.asList("Alice", "Bob", "Charlie"));

        assertEquals(3, project.totalMember());
        assertTrue(project.containsMember("Alice"));
        assertTrue(project.containsMember("Bob"));
        assertTrue(project.containsMember("Charlie"));
    }

    // ==================== record management ====================

    @Test
    public void addExpenseRecord_shouldUpdateStatus() {
        ExpenseProject project = createProjectWithMembers();
        ExpenseRecord record = createRecord(1, "Alice", 100);

        project.addExpenseRecord(record);

        assertEquals(1, project.listAllExpenseRecord().size());
    }

    @Test
    public void updateExpenseRecord_existingRecord_shouldUpdateInfo() {
        ExpenseProject project = createProjectWithMembers();
        ExpenseRecord record = createRecord(1, "Alice", 100);
        project.addExpenseRecord(record);

        ExpenseRecord updateRecord = createRecord(1, "Bob", 200);
        updateRecord.addConsumer("Charlie");
        boolean updated = project.updateExpenseRecord(updateRecord);

        assertTrue(updated);
        assertEquals("Bob", project.listAllExpenseRecord().get(0).getPayMember());
        assertEquals(0, new BigDecimal(200).compareTo(project.listAllExpenseRecord().get(0).getAmount()));
    }

    @Test
    public void updateExpenseRecord_noChange_shouldNotUpdateStatus() {
        ExpenseProject project = createProjectWithMembers();
        ExpenseRecord record = createRecord(1, "Alice", 100);
        project.addExpenseRecord(record);

        ExpenseRecord updateRecord = createRecord(1, "Alice", 100);
        boolean updated = project.updateExpenseRecord(updateRecord);

        assertFalse(updated);
    }

    @Test(expected = BizException.class)
    public void updateExpenseRecord_nonExisting_shouldThrow() {
        ExpenseProject project = createProjectWithMembers();
        ExpenseRecord updateRecord = createRecord(999, "Alice", 100);
        project.updateExpenseRecord(updateRecord);
    }

    @Test
    public void removeRecord_existing_shouldRemove() {
        ExpenseProject project = createProjectWithMembers();
        ExpenseRecord record = createRecord(1, "Alice", 100);
        project.addExpenseRecord(record);

        project.removeRecord(1);

        assertEquals(0, project.listAllExpenseRecord().size());
    }

    @Test(expected = BizException.class)
    public void removeRecord_nonExisting_shouldThrow() {
        ExpenseProject project = createProjectWithMembers();
        project.removeRecord(999);
    }

    // ==================== calcMemberSharingFee ====================

    @Test
    public void calcMemberSharingFee_singleRecord_equalSplit_shouldCalculateCorrectly() {
        ExpenseProject project = createProjectWithMembers();
        // Alice 付了 90，Alice/Bob/Charlie 三人消费
        ExpenseRecord record = new ExpenseRecord();
        record.setId(1);
        record.setProjectId(1);
        record.setPayMember("Alice");
        record.setAmount(new BigDecimal("90"));
        record.setDate(new java.util.Date());
        record.setExpenseType("餐饮");
        record.addConsumer("Alice");
        record.addConsumer("Bob");
        record.addConsumer("Charlie");
        project.addExpenseRecord(record);

        ProjectSharingFee fee = project.calcMemberSharingFee();
        List<MemberProjectFee> memberFees = fee.listMemberProjectFee();

        assertEquals(3, memberFees.size());

        for (MemberProjectFee memberFee : memberFees) {
            if ("Alice".equals(memberFee.getMember())) {
                // Alice 付了 90，消费 30 → 净应收 60
                assertEquals(0, new BigDecimal("90").compareTo(memberFee.getPaidAmount()));
                assertEquals(0, new BigDecimal("30").compareTo(memberFee.getConsumeAmount()));
            } else {
                // Bob/Charlie 没付，消费 30 → 净应付 30
                assertEquals(0, BigDecimal.ZERO.compareTo(memberFee.getPaidAmount()));
                assertEquals(0, new BigDecimal("30").compareTo(memberFee.getConsumeAmount()));
            }
        }
    }

    @Test
    public void calcMemberSharingFee_payerNotConsumer_shouldIncludePayer() {
        ExpenseProject project = createProjectWithMembers();
        // Alice 付了 100，但只有 Bob 消费（Alice 代付场景）
        ExpenseRecord record = new ExpenseRecord();
        record.setId(1);
        record.setProjectId(1);
        record.setPayMember("Alice");
        record.setAmount(new BigDecimal("100"));
        record.setDate(new java.util.Date());
        record.setExpenseType("餐饮");
        record.addConsumer("Bob");
        project.addExpenseRecord(record);

        ProjectSharingFee fee = project.calcMemberSharingFee();
        List<MemberProjectFee> memberFees = fee.listMemberProjectFee();

        assertEquals(2, memberFees.size());

        for (MemberProjectFee memberFee : memberFees) {
            if ("Alice".equals(memberFee.getMember())) {
                // Alice 付了 100，没消费 → 净应收 100
                assertEquals(0, new BigDecimal("100").compareTo(memberFee.getPaidAmount()));
                assertEquals(0, BigDecimal.ZERO.compareTo(memberFee.getConsumeAmount()));
            } else if ("Bob".equals(memberFee.getMember())) {
                // Bob 没付，消费 100 → 净应付 100
                assertEquals(0, BigDecimal.ZERO.compareTo(memberFee.getPaidAmount()));
                assertEquals(0, new BigDecimal("100").compareTo(memberFee.getConsumeAmount()));
            }
        }
    }

    @Test
    public void calcMemberSharingFee_multipleRecords_shouldAggregate() {
        ExpenseProject project = createProjectWithMembers();
        // Record 1: Alice 付 60，Alice/Bob 两人消费
        ExpenseRecord r1 = new ExpenseRecord();
        r1.setId(1);
        r1.setProjectId(1);
        r1.setPayMember("Alice");
        r1.setAmount(new BigDecimal("60"));
        r1.setDate(new java.util.Date());
        r1.setExpenseType("餐饮");
        r1.addConsumer("Alice");
        r1.addConsumer("Bob");
        project.addExpenseRecord(r1);

        // Record 2: Bob 付 40，Bob/Charlie 两人消费
        ExpenseRecord r2 = new ExpenseRecord();
        r2.setId(2);
        r2.setProjectId(1);
        r2.setPayMember("Bob");
        r2.setAmount(new BigDecimal("40"));
        r2.setDate(new java.util.Date());
        r2.setExpenseType("交通");
        r2.addConsumer("Bob");
        r2.addConsumer("Charlie");
        project.addExpenseRecord(r2);

        ProjectSharingFee fee = project.calcMemberSharingFee();
        List<MemberProjectFee> memberFees = fee.listMemberProjectFee();

        assertEquals(3, memberFees.size());

        for (MemberProjectFee memberFee : memberFees) {
            if ("Alice".equals(memberFee.getMember())) {
                // Alice 付了 60(R1)，消费了 30(R1 中) → 净应收 30
                assertEquals(0, new BigDecimal("60").compareTo(memberFee.getPaidAmount()));
                assertEquals(0, new BigDecimal("30").compareTo(memberFee.getConsumeAmount()));
            } else if ("Bob".equals(memberFee.getMember())) {
                // Bob 付了 40(R2)，消费了 30(R1)+20(R2)=50 → 净应付 10
                assertEquals(0, new BigDecimal("40").compareTo(memberFee.getPaidAmount()));
                assertEquals(0, new BigDecimal("50").compareTo(memberFee.getConsumeAmount()));
            } else if ("Charlie".equals(memberFee.getMember())) {
                // Charlie 没付，消费了 20(R2) → 净应付 20
                assertEquals(0, BigDecimal.ZERO.compareTo(memberFee.getPaidAmount()));
                assertEquals(0, new BigDecimal("20").compareTo(memberFee.getConsumeAmount()));
            }
        }
    }

    @Test
    public void calcMemberSharingFee_emptyRecords_shouldReturnEmpty() {
        ExpenseProject project = createProjectWithMembers();
        ProjectSharingFee fee = project.calcMemberSharingFee();
        assertTrue(fee.listMemberProjectFee().isEmpty());
    }

    @Test
    public void calcMemberSharingFee_unevenSplit_shouldHandleRounding() {
        ExpenseProject project = createProjectWithMembers();
        // 100 除以 3 人 = 33.333333（6位小数精度）
        ExpenseRecord record = new ExpenseRecord();
        record.setId(1);
        record.setProjectId(1);
        record.setPayMember("Alice");
        record.setAmount(new BigDecimal("100"));
        record.setDate(new java.util.Date());
        record.setExpenseType("餐饮");
        record.addConsumer("Alice");
        record.addConsumer("Bob");
        record.addConsumer("Charlie");
        project.addExpenseRecord(record);

        ProjectSharingFee fee = project.calcMemberSharingFee();
        List<MemberProjectFee> memberFees = fee.listMemberProjectFee();

        // 总消费应该接近 100（有舍入误差）
        BigDecimal totalConsume = memberFees.stream()
                .map(MemberProjectFee::getConsumeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 由于精度为6，误差应该非常小
        assertTrue(new BigDecimal("100").subtract(totalConsume).abs().compareTo(new BigDecimal("0.01")) < 0);
    }

    // ==================== isOwnedBy ====================

    @Test
    public void isOwnedBy_matchingUserId_shouldReturnTrue() {
        ExpenseProject project = new ExpenseProject();
        project.setCreateUserId(42);
        assertTrue(project.isOwnedBy(42));
    }

    @Test
    public void isOwnedBy_differentUserId_shouldReturnFalse() {
        ExpenseProject project = new ExpenseProject();
        project.setCreateUserId(42);
        assertFalse(project.isOwnedBy(99));
    }

    // ==================== totals ====================

    @Test
    public void totalExpense_multipleRecords_shouldSum() {
        ExpenseProject project = createProjectWithMembers();
        ExpenseRecord r1 = createRecord(1, "Alice", 50);
        ExpenseRecord r2 = createRecord(2, "Bob", 75);
        project.addExpenseRecord(r1);
        project.addExpenseRecord(r2);

        assertEquals(0, new BigDecimal("125").compareTo(project.totalExpense()));
    }

    @Test
    public void totalExpense_noRecords_shouldReturnZero() {
        ExpenseProject project = new ExpenseProject();
        assertEquals(0, BigDecimal.ZERO.compareTo(project.totalExpense()));
    }

    // ==================== validation ====================

    @Test(expected = BizException.class)
    public void checkUserIdExist_nullUserId_shouldThrow() {
        ExpenseProject project = new ExpenseProject();
        project.checkUserIdExist();
    }

    @Test
    public void checkUserIdExist_validUserId_shouldPass() {
        ExpenseProject project = new ExpenseProject();
        project.setCreateUserId(1);
        project.checkUserIdExist();
    }

    @Test(expected = BizException.class)
    public void checkProjectNameValid_blankName_shouldThrow() {
        ExpenseProject project = new ExpenseProject();
        project.checkProjectNameValid();
    }

    @Test(expected = BizException.class)
    public void checkProjectNameValid_tooLong_shouldThrow() {
        ExpenseProject project = new ExpenseProject();
        project.setName("a".repeat(100));
        project.checkProjectNameValid();
    }

    @Test
    public void checkProjectNameValid_validName_shouldPass() {
        ExpenseProject project = new ExpenseProject();
        project.setName("Valid Project");
        project.checkProjectNameValid();
    }

    // ==================== containsRecord ====================

    @Test
    public void containsRecord_existing_shouldReturnTrue() {
        ExpenseProject project = createProjectWithMembers();
        ExpenseRecord record = createRecord(1, "Alice", 100);
        project.addExpenseRecord(record);

        assertTrue(project.containsRecord(1));
    }

    @Test
    public void containsRecord_nonExisting_shouldReturnFalse() {
        ExpenseProject project = createProjectWithMembers();
        assertFalse(project.containsRecord(999));
    }

    // ==================== helpers ====================

    private ExpenseProject createProjectWithMembers() {
        ExpenseProject project = new ExpenseProject();
        project.setId(1);
        project.setCreateUserId(1);
        project.setName("Test Project");
        project.addMembers(Arrays.asList("Alice", "Bob", "Charlie"));
        return project;
    }

    private ExpenseRecord createRecord(int id, String payer, int amount) {
        ExpenseRecord record = new ExpenseRecord();
        record.setId(id);
        record.setProjectId(1);
        record.setPayMember(payer);
        record.setAmount(new BigDecimal(amount));
        record.setDate(new java.util.Date());
        record.setExpenseType("餐饮");
        record.setRemark("test");
        record.addConsumer("Alice");
        record.addConsumer("Bob");
        return record;
    }
}
