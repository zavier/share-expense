package com.github.zavier.expense;

import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.converter.ExpenseRecordDoConverter;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.user.UserDO;
import com.github.zavier.user.UserMapper;
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
    private ExpenseSharingMapper expenseSharingMapper;
    @Resource
    private UserMapper userMapper;

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
                .eq(ExpenseSharingDO::getRecordId, expenseRecord.getId())
                .delete();
        ExpenseRecordDoConverter.toExpenseSharingDOList(expenseRecord).forEach(expenseSharingDO -> {
            expenseSharingMapper.insertSelective(expenseSharingDO);
        });
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
        final List<ExpenseSharingDO> list = expenseSharingMapper.wrapper()
                .eq(ExpenseSharingDO::getRecordId, recordId)
                .list();
        list.forEach(expenseSharingDO -> {
            expenseRecord.addUserSharing(expenseSharingDO.getUserId(), expenseSharingDO.getWeight());
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
            userIdSet.add(expenseRecord.getCostUserId());
            expenseRecord.getUserIdSharingMap().values().forEach(expenseSharing -> {
                userIdSet.add(expenseSharing.getUserId());
            });
            return expenseRecord;
        }).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(userIdSet)) {
            // 查询用户名称，进行填充
            final Map<Integer, String> userIdNameMap = userMapper.wrapper()
                    .in(UserDO::getId, new ArrayList<>(userIdSet))
                    .list()
                    .stream()
                    .collect(Collectors.toMap(UserDO::getId, UserDO::getUserName, (v1, v2) -> v2));

            expenseRecords.forEach(expenseRecord -> {
                expenseRecord.setCostUserName(userIdNameMap.get(expenseRecord.getCostUserId()));
                expenseRecord.getUserIdSharingMap().values().forEach(expenseSharing -> {
                    expenseSharing.setUserName(userIdNameMap.get(expenseSharing.getUserId()));
                });
            });
        }

        return expenseRecords;
    }
}
