package com.github.zavier.converter;

import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.dto.ExpenseRecordAddCmd;

import java.util.Date;
import java.util.Optional;

public class ExpenseRecordConverter {

    public static ExpenseRecord toExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd) {
        final ExpenseRecord expenseRecord = new ExpenseRecord();
        expenseRecord.setUserId(expenseRecordAddCmd.getUserId());
        expenseRecord.setExpenseProjectId(expenseRecordAddCmd.getExpenseProjectId());
        expenseRecord.setAmount(expenseRecordAddCmd.getAmount());
        expenseRecord.setDate(Optional.ofNullable(expenseRecordAddCmd.getDate()).orElse(new Date()));
        expenseRecord.setExpenseType(expenseRecordAddCmd.getExpenseType());
        expenseRecord.setRemark(expenseRecordAddCmd.getRemark());

        expenseRecord.setChangingStatus(ChangingStatus.NEW);
        return expenseRecord;
    }
}
