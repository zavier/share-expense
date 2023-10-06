package com.github.zavier.validator;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Component
public class ExpenseRecordValidator {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    public void valid(ExpenseRecordAddCmd expenseRecordAddCmd) {
        Assert.notNull(expenseRecordAddCmd.getExpenseProjectId(), "项目ID不能为空");
        Assert.notNull(expenseRecordAddCmd.getUserId(), "创建人不能为空");
        Assert.notNull(expenseRecordAddCmd.getAmount(), "金额不能为空");
        Assert.notNull(expenseRecordAddCmd.getExpenseType(), "费用类型不能为空");

        Assert.isTrue(expenseRecordAddCmd.getAmount().compareTo(BigDecimal.ZERO) > 0, "金额必须大于0");
        Assert.isTrue(expenseRecordAddCmd.getAmount().scale() <= 2, "金额不能超过2位小数");

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(expenseRecordAddCmd.getExpenseProjectId());
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
    }
}
