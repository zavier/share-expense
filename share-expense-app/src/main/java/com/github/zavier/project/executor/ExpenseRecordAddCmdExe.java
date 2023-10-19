package com.github.zavier.project.executor;

import com.github.zavier.domain.expense.domainservice.ExpenseRecordConverter;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class ExpenseRecordAddCmdExe {

    @Resource
    private ExpenseRecordGateway expenseRecordGateway;
    @Resource
    private ExpenseRecordValidator expenseRecordValidator;
    @Resource
    private ExpenseRecordConverter expenseRecordConverter;

    public void execute(ExpenseRecordAddCmd expenseRecordAddCmd) {
        log.info("expenseRecordAddCmd: {}", expenseRecordAddCmd);
        expenseRecordValidator.valid(expenseRecordAddCmd);

        final ExpenseRecord expenseRecord = expenseRecordConverter.toExpenseRecord(expenseRecordAddCmd);
        expenseRecordGateway.save(expenseRecord);
    }
}
