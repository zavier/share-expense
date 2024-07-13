package com.github.zavier.converter;

import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.project.ExpenseProjectDO;

import java.util.Date;

public class ExpenseProjectConverter {

    public static ExpenseProjectDO toInsertDO(ExpenseProject expenseProject) {
        final ExpenseProjectDO expenseProjectDO = new ExpenseProjectDO();
        expenseProjectDO.setId(expenseProject.getId());
        expenseProjectDO.setName(expenseProject.getName());
        expenseProjectDO.setDescription(expenseProject.getDescription());
        expenseProjectDO.setCreateUserId(expenseProject.getCreateUserId());
        final Date now = new Date();
        expenseProjectDO.setVersion(0);
        expenseProjectDO.setLocked(false);
        expenseProjectDO.setCreatedAt(now);
        expenseProjectDO.setUpdatedAt(now);
        return expenseProjectDO;
    }

    public static ExpenseProjectDO toUpdateDO(ExpenseProject expenseProject) {
        final ExpenseProjectDO expenseProjectDO = new ExpenseProjectDO();
        expenseProjectDO.setCreateUserId(expenseProject.getCreateUserId());
        expenseProjectDO.setName(expenseProject.getName());
        expenseProjectDO.setDescription(expenseProject.getDescription());
        expenseProjectDO.setLocked(expenseProject.getLocked());
        expenseProjectDO.setUpdatedAt(new Date());
        expenseProjectDO.setVersion(expenseProject.getVersion() + 1);
        return expenseProjectDO;
    }
}
