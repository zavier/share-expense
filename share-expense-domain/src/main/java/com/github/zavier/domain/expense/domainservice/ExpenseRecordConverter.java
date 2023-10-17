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

    @Resource
    private UserGateway userGateway;

    public ExpenseRecord toExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd) {
        final ExpenseRecord expenseRecord = new ExpenseRecord();
        expenseRecord.setCostUserId(expenseRecordAddCmd.getUserId());
        expenseRecord.setProjectId(expenseRecordAddCmd.getProjectId());
        expenseRecord.setAmount(expenseRecordAddCmd.getAmount());
        expenseRecord.setDate(Optional.ofNullable(expenseRecordAddCmd.getDate()).orElse(new Date()));
        expenseRecord.setExpenseType(expenseRecordAddCmd.getExpenseType());
        expenseRecord.setRemark(expenseRecordAddCmd.getRemark());

        final Optional<User> userOpt = userGateway.getUserById(expenseRecord.getCostUserId());
        Assert.isTrue(userOpt.isPresent(), "用户ID不存在");

        expenseRecord.setCostUserId(expenseRecordAddCmd.getUserId());
        expenseRecord.setCostUserName(userOpt.get().getUserName());

        expenseRecord.setChangingStatus(ChangingStatus.NEW);
        return expenseRecord;
    }
}
