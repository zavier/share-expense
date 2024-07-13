package com.github.zavier.domain.expense.domainservice;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

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
}
