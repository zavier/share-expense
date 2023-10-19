package com.github.zavier.project.executor.converter;

import com.github.zavier.domain.expense.ExpenseProjectMember;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ExpenseRecordDTO;

public class ProjectConverter {

    public static ExpenseProjectMemberDTO convertToDTO(ExpenseProjectMember expenseProjectMember){
        final ExpenseProjectMemberDTO memberDTO = new ExpenseProjectMemberDTO();
        memberDTO.setProjectId(expenseProjectMember.getProjectId());
        memberDTO.setUserId(expenseProjectMember.getUserId());
        memberDTO.setUserName(expenseProjectMember.getUserName());
        memberDTO.setWeight(expenseProjectMember.getWeight());
        return memberDTO;
    }

    public static ExpenseRecordDTO convertToDTO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDTO expenseRecordDTO = new ExpenseRecordDTO();
        expenseRecordDTO.setRecordId(expenseRecord.getId());
        expenseRecordDTO.setUserId(expenseRecord.getCostUserId());
        expenseRecordDTO.setUserName(expenseRecord.getCostUserName());
        expenseRecordDTO.setExpenseProjectId(expenseRecord.getProjectId());
        expenseRecordDTO.setAmount(expenseRecord.getAmount());
        expenseRecordDTO.setDate(expenseRecord.getDate());
        expenseRecordDTO.setExpenseType(expenseRecord.getExpenseType());
        expenseRecordDTO.setRemark(expenseRecord.getRemark());
        return expenseRecordDTO;
    }

}
