package com.github.zavier.project.executor;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordConverter;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordValidator;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordConverter;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordValidator;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class ExpenseRecordAddCmdExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;
    @Resource
    private ExpenseRecordValidator expenseRecordValidator;
    @Resource
    private ExpenseRecordConverter expenseRecordConverter;

    public void execute(ExpenseRecordAddCmd expenseRecordAddCmd) {
        log.info("expenseRecordAddCmd: {}", expenseRecordAddCmd);
        expenseRecordValidator.valid(expenseRecordAddCmd);

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(expenseRecordAddCmd.getProjectId());
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOpt.get();

        Assert.isTrue(Objects.equals(expenseProject.getCreateUserId(), expenseRecordAddCmd.getOperatorId()), "无权限");

        final ExpenseRecord expenseRecord = expenseRecordConverter.toExpenseRecord(expenseRecordAddCmd);
        expenseProject.addExpenseRecord(expenseRecord);

        // 设置变更类型
        expenseProject.setChangingStatus(ChangingStatus.UPDATED);
        expenseProject.setMemberChangingStatus(ChangingStatus.UNCHANGED);
        expenseProject.setRecordChangingStatus(ChangingStatus.UPDATED);

        expenseProjectGateway.save(expenseProject);
    }
}
