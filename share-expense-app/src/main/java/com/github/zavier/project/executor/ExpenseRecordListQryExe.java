package com.github.zavier.project.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ExpenseRecordQry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class ExpenseRecordListQryExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    public SingleResponse<List<ExpenseRecord>> execute(ExpenseRecordQry expenseRecordQry) {
        final Integer projectId = expenseRecordQry.getProjectId();

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectId);
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");

        if (!Objects.equals(expenseRecordQry.getOperatorId(), projectOpt.get().getCreateUserId())) {
            return SingleResponse.of(Collections.emptyList());
        }

        return SingleResponse.of(projectOpt.get().listAllExpenseRecord());
    }
}
