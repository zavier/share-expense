package com.github.zavier.expense.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.ExpenseRecordQry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class ExpenseRecordListQryExe {

    @Resource
    private ExpenseRecordGateway expenseRecordGateway;
    @Resource
    private UserGateway userGateway;

    public SingleResponse<List<ExpenseRecord>> execute(ExpenseRecordQry expenseRecordQry) {
        final Integer projectId = expenseRecordQry.getProjectId();
        Assert.notNull(projectId, "项目ID不能为空");
        final List<ExpenseRecord> expenseRecords = expenseRecordGateway.listRecord(projectId);
        return SingleResponse.of(expenseRecords);
    }
}
