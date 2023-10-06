package com.github.zavier.expense;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ExpenseService;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.dto.ExpenseRecordSharingAddCmd;
import com.github.zavier.expense.executor.ExpenseRecordAddCmdExe;
import com.github.zavier.expense.executor.ExpenseRecordSharingAddCmdExe;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@CatchAndLog
public class ExpenseServiceImpl implements ExpenseService {

    @Resource
    private ExpenseRecordAddCmdExe expenseRecordAddCmdExe;
    @Resource
    private ExpenseRecordSharingAddCmdExe expenseRecordSharingAddCmdExe;

    @Override
    public Response addExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd) {
        expenseRecordAddCmdExe.execute(expenseRecordAddCmd);
        return Response.buildSuccess();
    }

    @Override
    public Response addExpenseRecordSharing(ExpenseRecordSharingAddCmd sharingAddCmd) {
        expenseRecordSharingAddCmdExe.execute(sharingAddCmd);
        return Response.buildSuccess();
    }
}
