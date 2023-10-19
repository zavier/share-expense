package com.github.zavier.domain.expense;

import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.common.ChangingStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.*;

public class ExpenseProject {

    /**
     * 项目成员ID
     */
    private final Map<Integer, ExpenseProjectMember> userIdMap = new HashMap<>();

    /**
     * 费用记录
     */
    private final List<ExpenseRecord> expenseRecordList = new ArrayList<>();

    /**
     * 费用项目ID
     */
    @Getter
    @Setter
    private Integer id;

    /**
     * 创建者用户ID
     */
    @Getter
    @Setter
    private Integer userId;

    /**
     * 项目名称
     */
    @Getter
    @Setter
    private String name;

    /**
     * 项目描述
     */
    @Getter
    @Setter
    private String description;

    /**
     * 版本号
     */
    @Getter
    @Setter
    private Integer version;

    @Getter
    @Setter
    private ChangingStatus changingStatus = ChangingStatus.NEW;

    public List<ExpenseProjectMember> listMember() {
        return Collections.unmodifiableList(new ArrayList<>(userIdMap.values()));
    }

    public void addMember(Integer userId, String userName, Integer weight) {
        final ExpenseProjectMember projectMember = new ExpenseProjectMember()
                .setProjectId(this.getId())
                .setUserId(userId)
                .setUserName(userName)
                .setWeight(weight);
        final ExpenseProjectMember previous = userIdMap.putIfAbsent(userId, projectMember);
        if (previous != null) {
            throw new BizException("用户已存在");
        }
    }

    public boolean existMember(Integer userId) {
        return userIdMap.containsKey(userId);
    }


    public void checkUserIdExist() {
        Assert.notNull(userId, "创建人不能为空");
    }

    public void checkProjectNameValid() {
        Assert.notNull(name, "项目名称不能为空");
        Assert.isTrue(name.length() < 100, "项目名称长度不能超过100字");
    }

    public void addExpenseRecord(ExpenseRecord expenseRecord) {
        expenseRecordList.add(expenseRecord);
    }

    public void cala() {
        // TODO
        final BigDecimal totalAmount = expenseRecordList.stream().map(ExpenseRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final Integer totalWeight = this.listMember().stream().map(ExpenseProjectMember::getWeight)
                .reduce(0, Integer::sum);

//        userIdSharingMap.values().forEach(expenseSharing ->
//                expenseSharing.setAmount(amount.multiply(new BigDecimal(expenseSharing.getWeight())).divide(new BigDecimal(totalWeight), 2, RoundingMode.HALF_UP)));
    }
}