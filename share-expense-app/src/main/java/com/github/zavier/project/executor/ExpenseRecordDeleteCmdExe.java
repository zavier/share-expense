package com.github.zavier.project.executor;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordConverter;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordValidator;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ExpenseRecordDeleteCmd;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class ExpenseRecordDeleteCmdExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;
    @Resource
    private ExpenseRecordValidator expenseRecordValidator;
    @Resource
    private ExpenseRecordConverter expenseRecordConverter;

    public void execute(ExpenseRecordDeleteCmd expenseRecordDeleteCmd) {
        log.info("ExpenseRecordDeleteCmd:{}", expenseRecordDeleteCmd);
        Assert.notNull(expenseRecordDeleteCmd.getProjectId(), "项目id不能为空");
        Assert.notNull(expenseRecordDeleteCmd.getRecordId(), "记录id不能为空");
        Assert.notNull(expenseRecordDeleteCmd.getOperatorId(), "操作人不能为空");


        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(expenseRecordDeleteCmd.getProjectId());
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOpt.get();

        Assert.isTrue(Objects.equals(expenseProject.getCreateUserId(), expenseRecordDeleteCmd.getOperatorId()), "无权限");

        expenseProject.removeRecord(expenseRecordDeleteCmd.getRecordId());
        expenseProjectGateway.save(expenseProject);
    }
}
