package com.github.zavier.expense.executor;

import com.github.zavier.converter.ExpenseRecordConverter;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.validator.ExpenseRecordValidator;
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

    public void execute(ExpenseRecordAddCmd expenseRecordAddCmd) {
        log.info("expenseRecordAddCmd: {}", expenseRecordAddCmd);
        expenseRecordValidator.valid(expenseRecordAddCmd);

        final ExpenseRecord expenseRecord = ExpenseRecordConverter.toExpenseRecord(expenseRecordAddCmd);
        expenseRecordGateway.save(expenseRecord);
    }
}
