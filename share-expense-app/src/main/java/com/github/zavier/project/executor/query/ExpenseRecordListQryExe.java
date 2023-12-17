package com.github.zavier.project.executor.query;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.ExpenseRecordQry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ExpenseRecordListQryExe {

    private final ExpenseRecordGateway expenseRecordGateway;
    private final UserGateway userGateway;

    public ExpenseRecordListQryExe(ExpenseRecordGateway expenseRecordGateway, UserGateway userGateway) {
        this.expenseRecordGateway = expenseRecordGateway;
        this.userGateway = userGateway;
    }

    public SingleResponse<List<ExpenseRecord>> execute(ExpenseRecordQry expenseRecordQry) {
        final Integer projectId = expenseRecordQry.getProjectId();
        Assert.notNull(projectId, "项目ID不能为空");
        final List<ExpenseRecord> expenseRecords = expenseRecordGateway.listRecord(projectId);
        return SingleResponse.of(expenseRecords);
    }
}
