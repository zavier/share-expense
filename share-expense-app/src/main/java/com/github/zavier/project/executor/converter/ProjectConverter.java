package com.github.zavier.project.executor.converter;

import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ExpenseRecordDTO;

import java.util.List;
import java.util.stream.Collectors;

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

        final List<ExpenseProjectMemberDTO> collect = expenseRecord.listAllConsumers().stream()
                .map(it -> {
                    final ExpenseProjectMemberDTO memberDTO = new ExpenseProjectMemberDTO();
                    memberDTO.setMember(it);
                    return memberDTO;
                }).collect(Collectors.toList());
        expenseRecordDTO.setConsumeMembers(collect);
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
