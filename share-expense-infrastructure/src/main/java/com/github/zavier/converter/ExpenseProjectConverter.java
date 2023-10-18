package com.github.zavier.converter;

import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.project.ExpenseProjectDO;
import com.github.zavier.project.ExpenseProjectMemberDO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

public class ExpenseProjectConverter {

    public static ExpenseProject toEntity(ExpenseProjectDO expenseProjectDO, List<ExpenseProjectMemberDO> memberDOList) {
        final ExpenseProject expenseProject = new ExpenseProject();
        expenseProject.setId(expenseProjectDO.getId());
        expenseProject.setUserId(expenseProjectDO.getCreateUserId());
        expenseProject.setName(expenseProjectDO.getName());
        expenseProject.setDescription(expenseProjectDO.getDescription());
        expenseProject.setVersion(expenseProjectDO.getVersion());

        if (CollectionUtils.isNotEmpty(memberDOList)) {
            for (ExpenseProjectMemberDO memberDO : memberDOList) {
                expenseProject.addMember(memberDO.getUserId(), memberDO.getUserName(), memberDO.getWeight());
            }
        }
        return expenseProject;
    }

    public static ExpenseProjectDO toInsertDO(ExpenseProject expenseProject) {
        final ExpenseProjectDO expenseProjectDO = new ExpenseProjectDO();
        expenseProjectDO.setId(expenseProject.getId());
        expenseProjectDO.setName(expenseProject.getName());
        expenseProjectDO.setDescription(expenseProject.getDescription());
        expenseProjectDO.setCreateUserId(expenseProject.getUserId());
        final Date now = new Date();
        expenseProjectDO.setVersion(0);
        expenseProjectDO.setCreatedAt(now);
        expenseProjectDO.setUpdatedAt(now);
        return expenseProjectDO;
    }

    public static ExpenseProjectDO toUpdateDO(ExpenseProject expenseProject) {
        final ExpenseProjectDO expenseProjectDO = new ExpenseProjectDO();
        expenseProjectDO.setCreateUserId(expenseProject.getUserId());
        expenseProjectDO.setName(expenseProject.getName());
        expenseProjectDO.setDescription(expenseProject.getDescription());
        expenseProjectDO.setUpdatedAt(new Date());
        expenseProjectDO.setVersion(expenseProject.getVersion() + 1);
        return expenseProjectDO;
    }
}
