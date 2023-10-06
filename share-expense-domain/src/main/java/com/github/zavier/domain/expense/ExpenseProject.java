package com.github.zavier.domain.expense;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.common.ChangingStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExpenseProject {

    /**
     * 项目成员ID
     */
    private final List<Integer> memberIds = new ArrayList<>();

    /**
     * 费用项目ID
     */
    @Getter
    @Setter
    private Integer expenseProjectId;

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

    public List<Integer> getMemberIds() {
        return Collections.unmodifiableList(memberIds);
    }

    public void addMember(Integer userId) {
        Assert.isFalse(memberIds.contains(userId), "用户已存在");
        memberIds.add(userId);
    }

    public boolean existMember(Integer userId) {
        return memberIds.contains(userId);
    }


    public void checkUserIdExist() {
        Assert.notNull(userId, "创建人不能为空");
    }

    public void checkProjectNameValid() {
        Assert.notNull(name, "项目名称不能为空");
        Assert.isTrue(name.length() < 100, "项目名称长度不能超过100字");
    }

}