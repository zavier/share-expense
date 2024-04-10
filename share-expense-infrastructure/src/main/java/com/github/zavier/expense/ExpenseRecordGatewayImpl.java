package com.github.zavier.expense;

import com.github.zavier.converter.ExpenseRecordDoConverter;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class ExpenseRecordGatewayImpl implements ExpenseRecordGateway {

    @Resource
    private ExpenseRecordMapper expenseRecordMapper;
    @Resource
    private ExpenseRecordConsumerMapper expenseRecordConsumerMapper;
    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    @Override
    @Transactional
    public void save(ExpenseRecord expenseRecord) {
        final Integer recordId = saveExpenseRecord(expenseRecord);

        expenseRecord.setId(recordId);
        saveRecordConsumer(expenseRecord);
    }

    private void saveRecordConsumer(ExpenseRecord expenseRecord) {
        final Integer projectId = expenseRecord.getProjectId();
        final Integer id = expenseRecord.getId();

        expenseRecord.listAllConsumers().forEach(consumer -> {
            final ExpenseRecordConsumerDO consumerDO = new ExpenseRecordConsumerDO();
            consumerDO.setProjectId(projectId);
            consumerDO.setRecordId(id);
            consumerDO.setMember(consumer);
            consumerDO.setCreatedAt(new Date());
            consumerDO.setUpdatedAt(new Date());
            expenseRecordConsumerMapper.insertSelective(consumerDO);
        });
    }

    private Integer saveExpenseRecord(ExpenseRecord expenseRecord) {
        final ExpenseRecordDO insertExpenseRecordDO = ExpenseRecordDoConverter.toInsertExpenseRecordDO(expenseRecord);
        expenseRecordMapper.insertSelective(insertExpenseRecordDO);
        return insertExpenseRecordDO.getId();
    }

    @Override
    public Optional<ExpenseRecord> getRecordById(@NotNull Integer recordId) {
        final List<ExpenseRecordConsumerDO> consumerDOS = expenseRecordConsumerMapper.wrapper()
                .eq(ExpenseRecordConsumerDO::getRecordId, recordId)
                .list();

        return expenseRecordMapper.selectByPrimaryKey(recordId)
                .map(it -> ExpenseRecordDoConverter.toExpenseRecord(it, consumerDOS));
    }



    @Override
    public List<ExpenseRecord> listRecord(@NotNull Integer projectId) {
        final List<ExpenseRecordDO> list = expenseRecordMapper.wrapper()
                .eq(ExpenseRecordDO::getProjectId, projectId)
                .list();

        final List<Integer> recordIdList = list.stream().map(ExpenseRecordDO::getId).collect(Collectors.toList());
        final Map<Integer, List<ExpenseRecordConsumerDO>> recordIdConsumersMap = expenseRecordConsumerMapper.wrapper()
                .in(ExpenseRecordConsumerDO::getRecordId, recordIdList)
                .stream()
                .collect(Collectors.groupingBy(ExpenseRecordConsumerDO::getRecordId));

        return list.stream()
                .map(it -> ExpenseRecordDoConverter.toExpenseRecord(it, recordIdConsumersMap.get(it.getId())))
                .collect(Collectors.toList());
    }
}
