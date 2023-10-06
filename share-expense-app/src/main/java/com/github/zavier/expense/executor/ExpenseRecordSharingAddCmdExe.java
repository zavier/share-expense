package com.github.zavier.expense.executor;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordValidator;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.dto.ExpenseRecordSharingAddCmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

@Slf4j
@Component
public class ExpenseRecordSharingAddCmdExe {

    @Resource
    private ExpenseRecordGateway expenseRecordGateway;
    @Resource
    private ExpenseRecordValidator expenseRecordValidator;

    public void execute(ExpenseRecordSharingAddCmd sharingAddCmd) {
        expenseRecordValidator.valid(sharingAddCmd);

        final Optional<ExpenseRecord> expenseRecordOptional = expenseRecordGateway.getRecordById(sharingAddCmd.getExpenseRecordId());
        Assert.isTrue(expenseRecordOptional.isPresent(), "费用记录不存在");

        final ExpenseRecord expenseRecord = expenseRecordOptional.get();
        expenseRecord.addUserSharing(sharingAddCmd.getUserId(), sharingAddCmd.getWeight());

        expenseRecord.setChangingStatus(ChangingStatus.UPDATED);
        expenseRecord.calcSharingAmount();
        expenseRecordGateway.save(expenseRecord);
    }
}
