package com.github.zavier.domain.expense;

import com.alibaba.cola.exception.Assert;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class ExpenseRecord {

    /**
     * 费用消费的成员信息
     */
    private Set<String> consumeMembers = new HashSet<>();

    @Getter
    @Setter
    private Integer id;
    @Getter
    @Setter
    private Integer projectId;

    @Getter
    @Setter
    private String payMember;
    @Getter
    @Setter
    private BigDecimal amount;
    @Getter
    @Setter
    private Date date;
    @Getter
    @Setter
    private String expenseType;
    @Getter
    @Setter
    private String remark;

    /**
     * 是否需要分摊
     */
    @Getter
    @Setter
    private Boolean needSharding = false;

    /**
     * 计算费用记录中，每个成员的费用信息
     *
     * @return
     */
    public List<MemberRecordFee> calcMembersFeeInRecord() {
        final BigDecimal perMemberPayAmount = amount.divide(BigDecimal.valueOf(consumeMembers.size()), 6, RoundingMode.HALF_DOWN);
        return consumeMembers.stream()
                .map(member -> {
                    final MemberRecordFee memberFeeDetail = new MemberRecordFee();
                    memberFeeDetail.setMember(member);
                    memberFeeDetail.setExpenseRecord(this);
                    memberFeeDetail.setConsumeAmount(perMemberPayAmount);
                    memberFeeDetail.setPaidAmount(isPaidMember(member) ? amount : BigDecimal.ZERO);
                    return memberFeeDetail;
                }).collect(Collectors.toList());
    }

    private boolean isPaidMember(String member) {
        return Objects.equals(payMember, member);
    }

    public void addConsumer(String name) {
        final boolean add = consumeMembers.add(name);
        Assert.isTrue(add, "消费人已存在:" + name);
    }

    public void addConsumers(List<String> names) {
        Assert.notEmpty(names, "消费人不能为空");
        names.forEach(this::addConsumer);
    }

    public Set<String> listAllConsumers() {
        return Collections.unmodifiableSet(consumeMembers);
    }

    public boolean updateInfo(ExpenseRecord updateRecord) {
        Assert.notNull(updateRecord, "更新记录不能为空");
        Assert.isTrue(Objects.equals(projectId, updateRecord.getProjectId()), "所属项目不允许修改");
        Assert.isTrue(Objects.equals(id, updateRecord.getId()), "记录ID不一致");

        boolean update = false;
        if (!consumeMembers.equals(updateRecord.listAllConsumers())) {
            this.consumeMembers = updateRecord.listAllConsumers();
            update = true;
        }
        if (!Objects.equals(payMember, updateRecord.getPayMember())) {
            this.payMember = updateRecord.payMember;
            update = true;
        }
        if (!Objects.equals(amount, updateRecord.getAmount())) {
            this.amount = updateRecord.getAmount();
            update = true;
        }
        if (!Objects.equals(date, updateRecord.getDate())) {
            this.date = updateRecord.getDate();
            update = true;
        }
        if (!Objects.equals(expenseType, updateRecord.getExpenseType())) {
            this.expenseType = updateRecord.getExpenseType();
            update = true;
        }
        if (!Objects.equals(remark, updateRecord.getRemark())) {
            this.remark = updateRecord.getRemark();
            update = true;
        }
        return update;
    }

}