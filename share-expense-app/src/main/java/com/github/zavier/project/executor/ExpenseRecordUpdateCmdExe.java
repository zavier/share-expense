package com.github.zavier.project.executor;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordConverter;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordValidator;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ExpenseRecordUpdateCmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class ExpenseRecordUpdateCmdExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;
    @Resource
    private ExpenseRecordValidator expenseRecordValidator;
    @Resource
    private ExpenseRecordConverter expenseRecordConverter;

    public void execute(ExpenseRecordUpdateCmd expenseRecordUpdateCmd) {
        log.info("expenseRecordAddCmd: {}", expenseRecordUpdateCmd);
        expenseRecordValidator.valid(expenseRecordUpdateCmd);

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(expenseRecordUpdateCmd.getProjectId());
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOpt.get();

        Assert.isTrue(Objects.equals(expenseProject.getCreateUserId(), expenseRecordUpdateCmd.getOperatorId()), "无权限");

        final ExpenseRecord expenseRecord = expenseRecordConverter.toExpenseRecord(expenseRecordUpdateCmd);
        expenseProject.updateExpenseRecord(expenseRecord);

        expenseProjectGateway.save(expenseProject);
    }
}
