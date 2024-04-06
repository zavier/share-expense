package com.github.zavier.project.executor.converter;

import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseProjectMember;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ProjectConverter {

    public static ExpenseProjectMemberDTO convertToDTO(ExpenseProjectMember expenseProjectMember){
        final ExpenseProjectMemberDTO memberDTO = new ExpenseProjectMemberDTO();
        memberDTO.setProjectId(expenseProjectMember.getProjectId());
        memberDTO.setUserId(expenseProjectMember.getUserId());
        memberDTO.setUserName(expenseProjectMember.getUserName());
        return memberDTO;
    }

    public static ExpenseRecordDTO convertToDTO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDTO expenseRecordDTO = new ExpenseRecordDTO();
        expenseRecordDTO.setRecordId(expenseRecord.getId());
        expenseRecordDTO.setUserId(expenseRecord.getPayUserId());
        expenseRecordDTO.setUserName(expenseRecord.getPayUserName());
        expenseRecordDTO.setExpenseProjectId(expenseRecord.getProjectId());
        expenseRecordDTO.setAmount(expenseRecord.getAmount());
        expenseRecordDTO.setDate(expenseRecord.getDate());
        expenseRecordDTO.setExpenseType(expenseRecord.getExpenseType());
        expenseRecordDTO.setRemark(expenseRecord.getRemark());
        return expenseRecordDTO;
    }

    public static ExpenseProject convert2AddProject(ProjectAddCmd projectAddCmd) {
        final ExpenseProject expenseProject = new ExpenseProject();
        expenseProject.setUserId(projectAddCmd.getUserId());
        expenseProject.setName(projectAddCmd.getProjectName());
        expenseProject.setDescription(projectAddCmd.getProjectDesc());

        final String memberNames = projectAddCmd.getMembers();
        if (StringUtils.isNotBlank(memberNames)) {
            final List<String> members = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(memberNames);
            for (String member : members) {
                expenseProject.addVirtualMember(member);
            }
        }

        expenseProject.checkUserIdExist();
        expenseProject.checkProjectNameValid();

        expenseProject.setChangingStatus(ChangingStatus.NEW);
        return expenseProject;

    }

}
