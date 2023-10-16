package com.github.zavier.domain.expense.domainservice;

import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.dto.ExpenseRecordAddCmd;

import java.util.Date;
import java.util.Optional;

public class ExpenseRecordConverter {

    public static ExpenseRecord toExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd) {
        final ExpenseRecord expenseRecord = new ExpenseRecord();
        expenseRecord.setCostUserId(expenseRecordAddCmd.getUserId());
        expenseRecord.setProjectId(expenseRecordAddCmd.getProjectId());
        expenseRecord.setAmount(expenseRecordAddCmd.getAmount());
        expenseRecord.setDate(Optional.ofNullable(expenseRecordAddCmd.getDate()).orElse(new Date()));
        expenseRecord.setExpenseType(expenseRecordAddCmd.getExpenseType());
        expenseRecord.setRemark(expenseRecordAddCmd.getRemark());

        expenseRecord.setCostUserId(expenseRecordAddCmd.getUserId());
        expenseRecord.setCostUserName(expenseRecordAddCmd.getUserName());

        expenseRecord.setChangingStatus(ChangingStatus.NEW);
        return expenseRecord;
    }
}
