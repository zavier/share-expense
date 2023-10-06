package com.github.zavier.domain.expense.gateway;

import com.github.zavier.domain.expense.ExpenseRecord;

public interface ExpenseRecordGateway {
    void save(ExpenseRecord expenseRecord);
}
