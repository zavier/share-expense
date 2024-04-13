package com.github.zavier.builder;

import com.github.zavier.converter.ExpenseRecordDoConverter;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.expense.ExpenseRecordConsumerDO;
import com.github.zavier.expense.ExpenseRecordDO;
import com.github.zavier.project.ExpenseProjectDO;
import com.github.zavier.project.ExpenseProjectMemberDO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExpenseProjectBuilder {

    private ExpenseProjectDO expenseProjectDO;
    private List<ExpenseProjectMemberDO> memberDOList;
    private List<ExpenseRecordDO> recordDOList;
    private List<ExpenseRecordConsumerDO> expenseRecordConsumerDOList;

    public ExpenseProjectBuilder setExpenseProjectDO(ExpenseProjectDO expenseProjectDO) {
        this.expenseProjectDO = expenseProjectDO;
        return this;
    }

    public ExpenseProjectBuilder setMemberDOList(List<ExpenseProjectMemberDO> memberDOList) {
        this.memberDOList = memberDOList;
        return this;
    }

    public ExpenseProjectBuilder setRecordDOList(List<ExpenseRecordDO> recordDOList) {
        this.recordDOList = recordDOList;
        return this;
    }

    public ExpenseProjectBuilder setExpenseRecordConsumerDOList(List<ExpenseRecordConsumerDO> expenseRecordConsumerDOList) {
        this.expenseRecordConsumerDOList = expenseRecordConsumerDOList;
        return this;
    }

    public ExpenseProject build() {
        final ExpenseProject expenseProject = new ExpenseProject();
        expenseProject.setId(expenseProjectDO.getId());
        expenseProject.setCreateUserId(expenseProjectDO.getCreateUserId());
        expenseProject.setName(expenseProjectDO.getName());
        expenseProject.setDescription(expenseProjectDO.getDescription());
        expenseProject.setVersion(expenseProjectDO.getVersion());

        if (CollectionUtils.isNotEmpty(memberDOList)) {
            for (ExpenseProjectMemberDO memberDO : memberDOList) {
                expenseProject.addMember(memberDO.getName());
            }
        }

        if (CollectionUtils.isNotEmpty(recordDOList) && CollectionUtils.isNotEmpty(expenseRecordConsumerDOList)) {
            final Map<Integer, List<ExpenseRecordConsumerDO>> recordIdMap = expenseRecordConsumerDOList.stream()
                    .collect(Collectors.groupingBy(ExpenseRecordConsumerDO::getRecordId));
            for (ExpenseRecordDO recordDO : recordDOList) {
                final ExpenseRecord expenseRecord = ExpenseRecordDoConverter.toExpenseRecord(recordDO, recordIdMap.get(recordDO.getId()));
                expenseProject.addExpenseRecord(expenseRecord);
            }
        }

        return expenseProject;
    }
}
