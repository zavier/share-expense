package com.github.zavier.converter;

import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.expense.ExpenseRecordConsumerDO;
import com.github.zavier.expense.ExpenseRecordDO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

public class ExpenseRecordDoConverter {

    public static ExpenseRecordDO toInsertExpenseRecordDO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDO expenseRecordDO = new ExpenseRecordDO();
        expenseRecordDO.setPayMember(expenseRecord.getPayMember());
        expenseRecordDO.setProjectId(expenseRecord.getProjectId());
        expenseRecordDO.setAmount(expenseRecord.getAmount());
        expenseRecordDO.setPayDate(expenseRecord.getDate());
        expenseRecordDO.setExpenseType(expenseRecord.getExpenseType());
        expenseRecordDO.setRemark(expenseRecord.getRemark());
        final Date now = new Date();
        expenseRecordDO.setCreatedAt(now);
        expenseRecordDO.setUpdatedAt(now);
        return expenseRecordDO;
    }

    public static ExpenseRecord toExpenseRecord(ExpenseRecordDO expenseRecordDO, List<ExpenseRecordConsumerDO> consumerDOS) {
        final ExpenseRecord expenseRecord = new ExpenseRecord();
        expenseRecord.setId(expenseRecordDO.getId());
        expenseRecord.setProjectId(expenseRecordDO.getProjectId());
        expenseRecord.setPayMember(expenseRecordDO.getPayMember());
        expenseRecord.setAmount(expenseRecordDO.getAmount());
        expenseRecord.setDate(expenseRecordDO.getPayDate());
        expenseRecord.setExpenseType(expenseRecordDO.getExpenseType());
        expenseRecord.setRemark(expenseRecordDO.getRemark());

        if (CollectionUtils.isNotEmpty(consumerDOS)) {
            consumerDOS.stream()
                    .map(ExpenseRecordConsumerDO::getMember)
                    .forEach(expenseRecord::addConsumer);
        }
        return expenseRecord;
    }
}
