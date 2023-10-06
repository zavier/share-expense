package com.github.zavier.api;

import com.alibaba.cola.dto.Response;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.dto.ExpenseRecordSharingAddCmd;

public interface ExpenseService {
    Response addExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd);

    Response addExpenseRecordSharing(ExpenseRecordSharingAddCmd sharingAddCmd);
}
