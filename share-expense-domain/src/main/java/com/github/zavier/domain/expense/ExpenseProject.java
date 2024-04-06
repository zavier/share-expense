package com.github.zavier.domain.expense;

import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.common.ChangingStatus;
import lombok.Getter;
import lombok.Setter;

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

    private Integer maxVirtualUserId = -1;

    public List<ExpenseProjectMember> listMember() {
        return Collections.unmodifiableList(new ArrayList<>(userIdMap.values()));
    }

    public void addMember(Integer userId, String userName, boolean isVirtual) {
        Assert.isFalse(existMember(userName), "用户已存在");
        if (isVirtual) {
            addVirtualMember(userName);
        } else {
            addActualMember(userId, userName);
        }
    }

    public void addActualMember(Integer userId, String userName) {
        final ExpenseProjectMember projectMember = new ExpenseProjectMember()
                .setProjectId(this.getId())
                .setUserId(userId)
                .setUserName(userName)
                .setIsVirtual(false);
        final ExpenseProjectMember previous = userIdMap.putIfAbsent(userId, projectMember);
        if (previous != null) {
            throw new BizException("用户已存在");
        }
    }

    public void addVirtualMember(String userName) {
        final ExpenseProjectMember projectMember = new ExpenseProjectMember()
                .setProjectId(this.getId())
                .setUserId(maxVirtualUserId--)
                .setUserName(userName)
                .setIsVirtual(true);
        final ExpenseProjectMember previous = userIdMap.putIfAbsent(projectMember.getUserId(), projectMember);
        if (previous != null) {
            throw new BizException("用户已存在");
        }
    }

    public boolean existMember(Integer userId) {
        return userIdMap.containsKey(userId);
    }

    public Optional<ExpenseProjectMember> getMember(Integer userId) {
        return Optional.ofNullable(userIdMap.get(userId));
    }

    public boolean existMember(String userName) {
        if (userIdMap.isEmpty()) {
            return false;
        }
        return userIdMap.values().stream()
                .map(ExpenseProjectMember::getUserName)
                .anyMatch(it -> it.equals(userName));
    }


    public void checkUserIdExist() {
        Assert.notNull(userId, "创建人不能为空");
    }


    public void checkProjectNameValid() {
        Assert.notNull(name, "项目名称不能为空");
        Assert.isTrue(name.length() < 100, "项目名称长度不能超过100字");
    }

}