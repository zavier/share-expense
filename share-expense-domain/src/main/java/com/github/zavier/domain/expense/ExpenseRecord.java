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
    private final Set<String> consumeMembers = new HashSet<>();

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

    public List<MemberFee> calcSharingFee() {
        final BigDecimal perMemberPayAmount = amount.divide(BigDecimal.valueOf(consumeMembers.size()), 6, RoundingMode.HALF_DOWN);
        return consumeMembers.stream()
                .map(member -> {
                    final MemberFee memberFee = new MemberFee();
                    memberFee.setMember(member);
                    memberFee.setRecordAmount(amount);
                    memberFee.setConsumeAmount(perMemberPayAmount);
                    memberFee.setPaidAmount(isPaidMember(member) ? amount : BigDecimal.ZERO);
                    return memberFee;
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

}