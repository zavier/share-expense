package com.github.zavier.expense;

import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.converter.ExpenseRecordDoConverter;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseProjectMember;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
        expenseRecord.getConsumerIdMap().forEach((k, v) -> {
            final ExpenseRecordConsumerDO consumerDO = new ExpenseRecordConsumerDO();
            consumerDO.setProjectId(projectId);
            consumerDO.setRecordId(id);
            consumerDO.setConsumerId(k);
            consumerDO.setConsumerName(v);
            consumerDO.setCreatedAt(new Date());
            consumerDO.setUpdatedAt(new Date());
            expenseRecordConsumerMapper.insertSelective(consumerDO);
        });
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

    @Override
    public Optional<ExpenseRecord> getRecordById(@NotNull Integer recordId) {
        final Optional<ExpenseRecord> expenseRecordOpt = expenseRecordMapper.selectByPrimaryKey(recordId)
                .map(ExpenseRecordDoConverter::toExpenseRecord);

        expenseRecordOpt.ifPresent(expenseRecord -> {
            wrapUserSharingDetail(recordId, expenseRecord);
        });

        return expenseRecordOpt;
    }

    private void wrapUserSharingDetail(@NotNull Integer recordId, ExpenseRecord expenseRecord) {
        final List<ExpenseRecordConsumerDO> list = expenseRecordConsumerMapper.wrapper()
                .eq(ExpenseRecordConsumerDO::getRecordId, recordId)
                .list();

        list.forEach(expenseSharingDO -> {
            expenseRecord.addUserSharing(expenseSharingDO.getConsumerId(),
                    expenseSharingDO.getConsumerName(), 1);
        });
    }

    @Override
    public List<ExpenseRecord> listRecord(@NotNull Integer projectId) {
        final List<ExpenseRecordDO> list = expenseRecordMapper.wrapper()
                .eq(ExpenseRecordDO::getProjectId, projectId)
                .list();
        Set<Integer> userIdSet = new HashSet<>();

        final List<ExpenseRecord> expenseRecords = list.stream().map(expenseRecordDO -> {
            final ExpenseRecord expenseRecord = ExpenseRecordDoConverter.toExpenseRecord(expenseRecordDO);
            wrapUserSharingDetail(expenseRecordDO.getId(), expenseRecord);

            // 获取用户ID
            userIdSet.add(expenseRecord.getPayUserId());
            expenseRecord.getUserIdSharingMap().values().forEach(expenseSharing -> {
                userIdSet.add(expenseSharing.getUserId());
            });
            return expenseRecord;
        }).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(userIdSet)) {
            final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectId);
            Assert.isTrue(projectOpt.isPresent(), "项目不存在");
            final ExpenseProject expenseProject = projectOpt.get();

            expenseRecords.forEach(expenseRecord -> {
                expenseRecord.setPayUserName(
                        expenseProject.getMember(expenseRecord.getPayUserId()).map(ExpenseProjectMember::getUserName).orElse(""));
                expenseRecord.getUserIdSharingMap().values().forEach(expenseSharing -> {
                    expenseSharing.setUserName(
                            expenseProject.getMember(expenseSharing.getUserId()).map(ExpenseProjectMember::getUserName).orElse(""));
                });
            });
        }

        return expenseRecords;
    }
}
