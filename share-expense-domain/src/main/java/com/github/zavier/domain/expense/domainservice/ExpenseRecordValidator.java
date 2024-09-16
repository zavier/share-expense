package com.github.zavier.domain.expense.domainservice;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.dto.ExpenseRecordUpdateCmd;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Optional;

@Component
public class ExpenseRecordValidator {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    public void valid(ExpenseRecordAddCmd expenseRecordAddCmd) {
        // 基础数据校验
        recordAddBaseCheck(expenseRecordAddCmd);
        // 项目存在校验
        final ExpenseProject expenseProject = projectIsExist(expenseRecordAddCmd.getProjectId());
        // 用户在项目组中
        userInProject(expenseRecordAddCmd, expenseProject);
    }

    public void valid(ExpenseRecordUpdateCmd expenseRecordUpdateCmd) {
        // 基础数据校验
        recordAddBaseCheck(expenseRecordUpdateCmd);
        Assert.notNull(expenseRecordUpdateCmd.getRecordId(), "修改的记录id不能为空");

        // 项目id及记录id校验（不能修改）
        final ExpenseProject expenseProject = projectIsExist(expenseRecordUpdateCmd.getProjectId());
        Assert.isTrue(expenseProject.containsRecord(expenseRecordUpdateCmd.getRecordId()), "该记录不存在");

        userInProject(expenseRecordUpdateCmd, expenseProject);
    }

    private static void userInProject(ExpenseRecordAddCmd expenseRecordAddCmd, ExpenseProject expenseProject) {
        Assert.isTrue(expenseProject.containsMember(expenseRecordAddCmd.getPayMember()), "用户不在项目组中:" + expenseRecordAddCmd.getPayMember());
        expenseRecordAddCmd.getConsumerMembers()
                .forEach(it -> Assert.isTrue(expenseProject.containsMember(it), "用户不在项目组中:" + it));
    }

    private static void recordAddBaseCheck(ExpenseRecordAddCmd expenseRecordAddCmd) {
        Assert.notNull(expenseRecordAddCmd.getProjectId(), "项目ID不能为空");
        Assert.notNull(expenseRecordAddCmd.getPayMember(), "花费人ID不能为空");
        Assert.notEmpty(expenseRecordAddCmd.getConsumerMembers(), "消费用户信息不能为空");
        Assert.notNull(expenseRecordAddCmd.getAmount(), "金额不能为空");
        Assert.notNull(expenseRecordAddCmd.getExpenseType(), "费用类型不能为空");

        Assert.isTrue(expenseRecordAddCmd.getAmount().compareTo(BigDecimal.ZERO) > 0, "金额必须大于0");
        Assert.isTrue(expenseRecordAddCmd.getAmount().scale() <= 2, "金额不能超过2位小数");
    }

    private ExpenseProject projectIsExist(@NotNull Integer projectId) {
        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectId);
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
        return projectOpt.get();
    }
}
