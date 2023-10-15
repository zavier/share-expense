package com.github.zavier.api;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.dto.ExpenseRecordQry;
import com.github.zavier.dto.ExpenseRecordSharingAddCmd;
import com.github.zavier.dto.data.ExpenseRecordDTO;

import java.util.List;

public interface ExpenseService {
    Response addExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd);

    SingleResponse<List<ExpenseRecordDTO>> listRecord(ExpenseRecordQry expenseRecordQry);

    Response addExpenseRecordSharing(ExpenseRecordSharingAddCmd sharingAddCmd);
}
