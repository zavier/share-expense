package com.github.zavier.domain.expense.domainservice;

import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseProjectMember;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
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
    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    public ExpenseRecord toExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd) {
        final ExpenseRecord expenseRecord = new ExpenseRecord();
        expenseRecord.setPayUserId(expenseRecordAddCmd.getPayUserId());
        expenseRecord.setProjectId(expenseRecordAddCmd.getProjectId());
        expenseRecord.setAmount(expenseRecordAddCmd.getAmount());
        expenseRecord.setDate(Optional.ofNullable(expenseRecordAddCmd.getDate()).orElse(new Date()));
        expenseRecord.setExpenseType(expenseRecordAddCmd.getExpenseType());
        expenseRecord.setRemark(expenseRecordAddCmd.getRemark());

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(expenseRecord.getProjectId());
        Assert.isTrue(projectOpt.isPresent(), "项目ID不存在");
        final ExpenseProject expenseProject = projectOpt.get();

        // 支付用户信息
        final ExpenseProjectMember member =
                expenseProject.getMember(expenseRecordAddCmd.getPayUserId())
                        .orElseThrow(() -> new BizException("用户ID不存在:" + expenseRecord.getPayUserId()));
        expenseRecord.setPayUserName(member.getUserName());

        // 消费用户信息
        expenseRecordAddCmd.listConsumerIds().forEach(consumerId -> {
            final ExpenseProjectMember consumer =
                    expenseProject.getMember(consumerId)
                            .orElseThrow(() -> new BizException("用户ID不存在:" + consumerId));
            expenseRecord.addConsumer(consumer.getUserId(), consumer.getUserName());
        });

        expenseRecord.setChangingStatus(ChangingStatus.NEW);
        return expenseRecord;
    }
}
