package com.github.zavier.converter;

import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.expense.ExpenseRecordDO;

import java.util.Date;

public class ExpenseRecordDoConverter {

    public static ExpenseRecordDO toInsertExpenseRecordDO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDO expenseRecordDO = new ExpenseRecordDO();
        expenseRecordDO.setId(expenseRecord.getId());
        expenseRecordDO.setUserId(expenseRecord.getUserId());
        expenseRecordDO.setExpenseProjectId(expenseRecord.getExpenseProjectId());
        expenseRecordDO.setAmount(expenseRecord.getAmount());
        expenseRecordDO.setDate(expenseRecord.getDate());
        expenseRecordDO.setExpenseType(expenseRecord.getExpenseType());
        expenseRecordDO.setRemark(expenseRecord.getRemark());
        final Date now = new Date();
        expenseRecordDO.setCreatedAt(now);
        expenseRecordDO.setUpdatedAt(now);
        return expenseRecordDO;
    }
}
