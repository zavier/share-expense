package com.github.zavier.domain.expense.domainservice;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.dto.ExpenseRecordSharingAddCmd;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Optional;

@Component
public class ExpenseRecordValidator {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;
    @Resource
    private ExpenseRecordGateway expenseRecordGateway;
    @Resource
    private UserGateway userGateway;

    public void valid(ExpenseRecordAddCmd expenseRecordAddCmd) {
        // 基础数据校验
        recordAddBaseCheck(expenseRecordAddCmd);
        // 项目存在校验
        projectIsExist(expenseRecordAddCmd.getProjectId());
    }

    private static void recordAddBaseCheck(ExpenseRecordAddCmd expenseRecordAddCmd) {
        Assert.notNull(expenseRecordAddCmd.getProjectId(), "项目ID不能为空");
        Assert.notNull(expenseRecordAddCmd.getUserId(), "创建人不能为空");
        Assert.notNull(expenseRecordAddCmd.getAmount(), "金额不能为空");
        Assert.notNull(expenseRecordAddCmd.getExpenseType(), "费用类型不能为空");

        Assert.isTrue(expenseRecordAddCmd.getAmount().compareTo(BigDecimal.ZERO) > 0, "金额必须大于0");
        Assert.isTrue(expenseRecordAddCmd.getAmount().scale() <= 2, "金额不能超过2位小数");
    }

    public void valid(ExpenseRecordSharingAddCmd expenseRecordSharingAddCmd) {
        // 基础数据校验
        shardingAddBaseCheck(expenseRecordSharingAddCmd);
        // 费用记录存在校验
        recordIsExist(expenseRecordSharingAddCmd.getExpenseRecordId());
        // 用户存在校验
        userIsExist(expenseRecordSharingAddCmd.getUserId());
        // 分摊的用户在项目组中
        userIsInProject(expenseRecordSharingAddCmd.getUserId(), expenseRecordSharingAddCmd.getExpenseRecordId());
    }

    private static void shardingAddBaseCheck(ExpenseRecordSharingAddCmd expenseRecordSharingAddCmd) {
        Assert.notNull(expenseRecordSharingAddCmd.getExpenseRecordId(), "费用记录ID不能为空");
        Assert.notNull(expenseRecordSharingAddCmd.getUserId(), "创建人不能为空");
        Assert.notNull(expenseRecordSharingAddCmd.getWeight(), "权重不能为空");

        Assert.isTrue(expenseRecordSharingAddCmd.getWeight() > 0, "权重必须大于0");
    }

    private void projectIsExist(@NotNull Integer projectId) {
        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectId);
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
    }

    private void recordIsExist(@NotNull Integer recordId) {
        final Optional<ExpenseRecord> recordById = expenseRecordGateway.getRecordById(recordId);
        Assert.isTrue(recordById.isPresent(), "费用记录不存在");
    }

    private void userIsExist(@NotNull Integer userId) {
        final Optional<User> userById = userGateway.getUserById(userId);
        Assert.isTrue(userById.isPresent(), "用户不存在");
    }

    private void userIsInProject(@NotNull Integer userId, @NotNull Integer expenseRecordId) {
        final Optional<ExpenseRecord> recordById = expenseRecordGateway.getRecordById(expenseRecordId);
        Assert.isTrue(recordById.isPresent(), "费用记录不存在");

        final Integer expenseProjectId = recordById.get().getExpenseProjectId();
        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(expenseProjectId);
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");

        final ExpenseProject expenseProject = projectOpt.get();
        Assert.isTrue(expenseProject.existMember(userId), "要分摊的用户不在项目组中");
    }
}
