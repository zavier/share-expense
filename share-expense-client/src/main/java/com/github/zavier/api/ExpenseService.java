package com.github.zavier.api;

import com.alibaba.cola.dto.Response;
import com.github.zavier.dto.ExpenseRecordAddCmd;

public interface ExpenseService {
    Response addExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd);
}
