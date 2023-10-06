package com.github.zavier.expense;

import com.alibaba.cola.exception.BizException;
import com.github.zavier.converter.ExpenseRecordDoConverter;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Slf4j
@Repository
public class ExpenseRecordGatewayImpl implements ExpenseRecordGateway {

    @Resource
    private ExpenseRecordMapper expenseRecordMapper;

    @Override
    public void save(ExpenseRecord expenseRecord) {
        final ChangingStatus changingStatus = expenseRecord.getChangingStatus();
        switch (changingStatus) {
            case NEW:
                final ExpenseRecordDO insertExpenseRecordDO = ExpenseRecordDoConverter.toInsertExpenseRecordDO(expenseRecord);
                expenseRecordMapper.insertSelective(insertExpenseRecordDO);
                break;
            default:
                throw new BizException("不支持的操作");
        }
    }

}
