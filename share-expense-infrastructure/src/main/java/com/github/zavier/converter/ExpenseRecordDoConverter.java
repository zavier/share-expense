package com.github.zavier.converter;

import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.expense.ExpenseRecordDO;

import java.util.Date;

public class ExpenseRecordDoConverter {

    public static ExpenseRecordDO toInsertExpenseRecordDO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDO expenseRecordDO = new ExpenseRecordDO();
        expenseRecordDO.setPayUserId(expenseRecord.getPayUserId());
        expenseRecordDO.setPayUserName(expenseRecord.getPayUserName());
        expenseRecordDO.setProjectId(expenseRecord.getProjectId());
        expenseRecordDO.setAmount(expenseRecord.getAmount());
        expenseRecordDO.setPayDate(expenseRecord.getDate());
        expenseRecordDO.setExpenseType(expenseRecord.getExpenseType());
        expenseRecordDO.setRemark(expenseRecord.getRemark());
        final Date now = new Date();
        expenseRecordDO.setCreatedAt(now);
        expenseRecordDO.setUpdatedAt(now);
        expenseRecordDO.setVersion(0);
        return expenseRecordDO;
    }



    public static ExpenseRecordDO toUpdateExpenseRecordDO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDO expenseRecordDO = new ExpenseRecordDO();
        expenseRecordDO.setId(expenseRecord.getId());
        expenseRecordDO.setPayUserId(expenseRecord.getPayUserId());
        expenseRecordDO.setPayUserName(expenseRecord.getPayUserName());
        expenseRecordDO.setProjectId(expenseRecord.getProjectId());
        expenseRecordDO.setAmount(expenseRecord.getAmount());
        expenseRecordDO.setPayDate(expenseRecord.getDate());
        expenseRecordDO.setExpenseType(expenseRecord.getExpenseType());
        expenseRecordDO.setRemark(expenseRecord.getRemark());
        final Date now = new Date();
        expenseRecordDO.setUpdatedAt(now);
        expenseRecordDO.setVersion(expenseRecord.getVersion() + 1);
        return expenseRecordDO;
    }

    public static ExpenseRecord toExpenseRecord(ExpenseRecordDO expenseRecordDO) {
        final ExpenseRecord expenseRecord = new ExpenseRecord();
        expenseRecord.setId(expenseRecordDO.getId());
        expenseRecord.setProjectId(expenseRecordDO.getProjectId());
        expenseRecord.setPayUserId(expenseRecordDO.getPayUserId());
        expenseRecord.setPayUserName(expenseRecordDO.getPayUserName());
        expenseRecord.setAmount(expenseRecordDO.getAmount());
        expenseRecord.setDate(expenseRecordDO.getPayDate());
        expenseRecord.setExpenseType(expenseRecordDO.getExpenseType());
        expenseRecord.setRemark(expenseRecordDO.getRemark());
        expenseRecord.setVersion(expenseRecordDO.getVersion());
        return expenseRecord;
    }
}
