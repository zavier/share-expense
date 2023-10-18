package com.github.zavier.expense.executor;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.ExpenseSharing;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.dto.ExpenseRecordSharingListQry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ExpenseRecordSharingListQryExe {

    @Resource
    private ExpenseRecordGateway expenseRecordGateway;

    public List<ExpenseSharing> execute(ExpenseRecordSharingListQry sharingListQry) {
        Assert.notNull(sharingListQry.getRecordId(), "记录ID不能为空");
        final Optional<ExpenseRecord> recordOpt = expenseRecordGateway.getRecordById(sharingListQry.getRecordId());
        Assert.isTrue(recordOpt.isPresent(), "记录不存在");
        final ExpenseRecord expenseRecord = recordOpt.get();

        return new ArrayList<>(expenseRecord.getUserIdSharingMap().values());

    }
}
