package com.github.zavier.domain.expense.domainservice;

import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.dto.ExpenseRecordUpdateCmd;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ExpenseRecordConverter {

    public ExpenseRecord toExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd) {
        final ExpenseRecord expenseRecord = new ExpenseRecord();
        expenseRecord.setPayMember(expenseRecordAddCmd.getPayMember());
        expenseRecord.setProjectId(expenseRecordAddCmd.getProjectId());
        expenseRecord.setAmount(expenseRecordAddCmd.getAmount());

        Date payDate = new Date(expenseRecordAddCmd.getDate() * 1000L);
        expenseRecord.setDate(payDate);
        expenseRecord.setExpenseType(expenseRecordAddCmd.getExpenseType());
        expenseRecord.setRemark(expenseRecordAddCmd.getRemark());

        // 消费用户信息
        expenseRecordAddCmd.getConsumerMembers().forEach(expenseRecord::addConsumer);

        return expenseRecord;
    }

    public ExpenseRecord toExpenseRecord(ExpenseRecordUpdateCmd expenseRecordUpdateCmd) {
        final ExpenseRecord expenseRecord = toExpenseRecord((ExpenseRecordAddCmd) expenseRecordUpdateCmd);
        expenseRecord.setId(expenseRecordUpdateCmd.getRecordId());
        return expenseRecord;
    }
}
