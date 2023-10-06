package com.github.zavier.expense;

import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.converter.ExpenseRecordDoConverter;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class ExpenseRecordGatewayImpl implements ExpenseRecordGateway {

    @Resource
    private ExpenseRecordMapper expenseRecordMapper;
    @Resource
    private ExpenseSharingMapper expenseSharingMapper;

    @Override
    @Transactional
    public void save(ExpenseRecord expenseRecord) {
        final Integer recordId = saveExpenseRecord(expenseRecord);

        expenseRecord.setId(recordId);
        saveExpenseRecordSharing(expenseRecord);
    }

    private Integer saveExpenseRecord(ExpenseRecord expenseRecord) {
        final ChangingStatus changingStatus = expenseRecord.getChangingStatus();
        switch (changingStatus) {
            case NEW:
                final ExpenseRecordDO insertExpenseRecordDO = ExpenseRecordDoConverter.toInsertExpenseRecordDO(expenseRecord);
                expenseRecordMapper.insertSelective(insertExpenseRecordDO);
                return insertExpenseRecordDO.getId();
            case UPDATED:
                final ExpenseRecordDO updateExpenseRecordDO = ExpenseRecordDoConverter.toUpdateExpenseRecordDO(expenseRecord);
                final int i = expenseRecordMapper.wrapper()
                        .eq(ExpenseRecordDO::getId, expenseRecord.getId())
                        .eq(ExpenseRecordDO::getVersion, expenseRecord.getVersion())
                        .updateSelective(updateExpenseRecordDO);
                Assert.isTrue(i == 1, "数据已被其他人操作，请刷新后重试");
                return expenseRecord.getId();
            default:
                throw new BizException("不支持的操作");
        }
    }

    private void saveExpenseRecordSharing(ExpenseRecord expenseRecord) {
        expenseSharingMapper.wrapper()
                .eq(ExpenseSharingDO::getExpenseRecordId, expenseRecord.getId())
                .delete();
        ExpenseRecordDoConverter.toExpenseSharingDOList(expenseRecord).forEach(expenseSharingDO -> {
            expenseSharingMapper.insertSelective(expenseSharingDO);
        });
    }

    @Override
    public Optional<ExpenseRecord> getRecordById(@NotNull Integer recordId) {
        final Optional<ExpenseRecord> expenseRecord = expenseRecordMapper.selectByPrimaryKey(recordId)
                .map(ExpenseRecordDoConverter::toExpenseRecord);

        expenseRecord.ifPresent(expenseRecordDO -> {
            final List<ExpenseSharingDO> list = expenseSharingMapper.wrapper()
                    .eq(ExpenseSharingDO::getExpenseRecordId, recordId)
                    .list();
            list.forEach(expenseSharingDO -> {
                expenseRecordDO.addUserSharing(expenseSharingDO.getUserId(), expenseSharingDO.getWeight());
            });
        });

        return expenseRecord;
    }

}
