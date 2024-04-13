package com.github.zavier.project.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.ExpenseRecordQry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ExpenseRecordListQryExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;
    @Resource
    private UserGateway userGateway;

    public SingleResponse<List<ExpenseRecord>> execute(ExpenseRecordQry expenseRecordQry) {
        final Integer projectId = expenseRecordQry.getProjectId();

        final Optional<ExpenseProject> projectById = expenseProjectGateway.getProjectById(projectId);
        Assert.isTrue(projectById.isPresent(), "项目不存在");
        return SingleResponse.of(projectById.get().listAllExpenseRecord());
    }
}
