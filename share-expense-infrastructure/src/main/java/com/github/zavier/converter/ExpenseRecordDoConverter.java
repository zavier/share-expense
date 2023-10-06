package com.github.zavier.converter;

import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.ExpenseSharing;
import com.github.zavier.expense.ExpenseRecordDO;
import com.github.zavier.expense.ExpenseSharingDO;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExpenseRecordDoConverter {

    public static ExpenseRecordDO toInsertExpenseRecordDO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDO expenseRecordDO = new ExpenseRecordDO();
        expenseRecordDO.setUserId(expenseRecord.getUserId());
        expenseRecordDO.setExpenseProjectId(expenseRecord.getExpenseProjectId());
        expenseRecordDO.setAmount(expenseRecord.getAmount());
        expenseRecordDO.setDate(expenseRecord.getDate());
        expenseRecordDO.setExpenseType(expenseRecord.getExpenseType());
        expenseRecordDO.setRemark(expenseRecord.getRemark());
        final Date now = new Date();
        expenseRecordDO.setCreatedAt(now);
        expenseRecordDO.setUpdatedAt(now);
        expenseRecordDO.setVersion(0);
        return expenseRecordDO;
    }

    public static List<ExpenseSharingDO> toExpenseSharingDOList(ExpenseRecord expenseRecord) {
        if (!expenseRecord.hasSharing()) {
            return Collections.emptyList();
        }

        final Map<Integer, ExpenseSharing> userIdSharingMap = expenseRecord.getUserIdSharingMap();
        return userIdSharingMap.values().stream().map(expenseSharing -> {
            final ExpenseSharingDO expenseSharingDO = new ExpenseSharingDO();
            expenseSharingDO.setExpenseRecordId(expenseRecord.getId());
            expenseSharingDO.setUserId(expenseSharing.getUserId());
            expenseSharingDO.setWeight(expenseSharing.getWeight());
            expenseSharingDO.setAmount(expenseSharing.getAmount());
            return expenseSharingDO;
        }).collect(Collectors.toList());
    }

    public static ExpenseRecordDO toUpdateExpenseRecordDO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDO expenseRecordDO = new ExpenseRecordDO();
        expenseRecordDO.setId(expenseRecord.getId());
        expenseRecordDO.setUserId(expenseRecord.getUserId());
        expenseRecordDO.setExpenseProjectId(expenseRecord.getExpenseProjectId());
        expenseRecordDO.setAmount(expenseRecord.getAmount());
        expenseRecordDO.setDate(expenseRecord.getDate());
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
        expenseRecord.setUserId(expenseRecordDO.getUserId());
        expenseRecord.setExpenseProjectId(expenseRecordDO.getExpenseProjectId());
        expenseRecord.setAmount(expenseRecordDO.getAmount());
        expenseRecord.setDate(expenseRecordDO.getDate());
        expenseRecord.setExpenseType(expenseRecordDO.getExpenseType());
        expenseRecord.setRemark(expenseRecordDO.getRemark());
        expenseRecord.setVersion(expenseRecordDO.getVersion());
        return expenseRecord;
    }
}
