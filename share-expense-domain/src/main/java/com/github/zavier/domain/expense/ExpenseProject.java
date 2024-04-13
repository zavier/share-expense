package com.github.zavier.domain.expense;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.common.ChangingStatus;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ExpenseProject {

    /**
     * 项目成员
     */
    private final Set<String> members = new HashSet<>();

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
    private Integer createUserId;

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

    /**
     * 版本号
     */
    @Getter
    @Setter
    private Boolean locked;

    @Getter
    @Setter
    private ChangingStatus changingStatus = ChangingStatus.NEW;

    @Getter
    @Setter
    private ChangingStatus recordChangingStatus = ChangingStatus.UNCHANGED;

    @Getter
    @Setter
    private ChangingStatus memberChangingStatus = ChangingStatus.UNCHANGED;


    public ProjectSharingFee calcMemberSharingFee() {
        // 计算每个成员的费用项的分摊
        final List<MemberRecordFee> memberFeeDetailList = expenseRecordList.stream()
                .map(ExpenseRecord::calcMembersFeeInRecord)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // 汇总
        final ProjectSharingFee projectFee = new ProjectSharingFee();
        memberFeeDetailList.forEach(projectFee::addMemberRecordFee);
        return projectFee;
    }


    public List<String> listAllMember() {
        return Collections.unmodifiableList(new ArrayList<>(members));
    }

    public List<ExpenseRecord> listAllExpenseRecord() {
        return Collections.unmodifiableList(new ArrayList<>(expenseRecordList));
    }

    public void addExpenseRecord(ExpenseRecord expenseRecord) {
        expenseRecordList.add(expenseRecord);

        if (recordChangingStatus == ChangingStatus.UNCHANGED) {
            recordChangingStatus = ChangingStatus.NEW;
        }
    }

    public void addMember(String name) {
        final boolean add = members.add(name);
        Assert.isTrue(add, "添加用户已存在:" + name);

        if (memberChangingStatus == ChangingStatus.UNCHANGED) {
            memberChangingStatus = ChangingStatus.NEW;
        }
    }

    public void addMembers(List<String> names) {
        if (names == null) {
            return;
        }
        names.forEach(this::addMember);
    }


    public boolean containsMember(String name) {
        return members.contains(name);
    }


    public void checkUserIdExist() {
        Assert.notNull(createUserId, "创建人不能为空");
    }


    public void checkProjectNameValid() {
        Assert.isTrue(StringUtils.isNotBlank(name), "项目名称不能为空");
        Assert.isTrue(name.length() < 100, "项目名称长度不能超过100字");
    }

}