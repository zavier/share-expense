package com.github.zavier.project.executor.converter;

import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.data.ExpenseRecordDTO;

import java.util.ArrayList;

public class ProjectConverter {

    public static ExpenseRecordDTO convertToDTO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDTO expenseRecordDTO = new ExpenseRecordDTO();
        expenseRecordDTO.setRecordId(expenseRecord.getId());
        expenseRecordDTO.setPayMember(expenseRecord.getPayMember());
        expenseRecordDTO.setExpenseProjectId(expenseRecord.getProjectId());
        expenseRecordDTO.setAmount(expenseRecord.getAmount());
        expenseRecordDTO.setDate(expenseRecord.getDate().getTime() / 1000);
        expenseRecordDTO.setExpenseType(expenseRecord.getExpenseType());
        expenseRecordDTO.setRemark(expenseRecord.getRemark());
        expenseRecordDTO.setConsumeMembers(new ArrayList<>(expenseRecord.listAllConsumers()));
        return expenseRecordDTO;
    }

    public static ExpenseProject convert2AddProject(ProjectAddCmd projectAddCmd) {
        final ExpenseProject expenseProject = new ExpenseProject();
        expenseProject.setCreateUserId(projectAddCmd.getCreateUserId());
        expenseProject.setName(projectAddCmd.getProjectName());
        expenseProject.setDescription(projectAddCmd.getProjectDesc());
        expenseProject.setLocked(false);
        expenseProject.setChangingStatus(ChangingStatus.NEW);

        expenseProject.addMembers(projectAddCmd.getMembers());
        expenseProject.checkUserIdExist();
        expenseProject.checkProjectNameValid();

        return expenseProject;

    }

}
