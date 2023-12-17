package com.github.zavier.project.executor;

import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordConverter;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordValidator;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExpenseRecordAddCmdExe {

    private final ExpenseRecordGateway expenseRecordGateway;
    private final ExpenseRecordValidator expenseRecordValidator;
    private final ExpenseRecordConverter expenseRecordConverter;

    public ExpenseRecordAddCmdExe(ExpenseRecordGateway expenseRecordGateway, ExpenseRecordValidator expenseRecordValidator, ExpenseRecordConverter expenseRecordConverter) {
        this.expenseRecordGateway = expenseRecordGateway;
        this.expenseRecordValidator = expenseRecordValidator;
        this.expenseRecordConverter = expenseRecordConverter;
    }

    public void execute(ExpenseRecordAddCmd expenseRecordAddCmd) {
        log.info("expenseRecordAddCmd: {}", expenseRecordAddCmd);
        expenseRecordValidator.valid(expenseRecordAddCmd);

        final ExpenseRecord expenseRecord = expenseRecordConverter.toExpenseRecord(expenseRecordAddCmd);
        expenseRecordGateway.save(expenseRecord);
    }
}
