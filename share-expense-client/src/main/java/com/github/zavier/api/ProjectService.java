package com.github.zavier.api;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.dto.*;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.dto.data.UserSharingDTO;

import java.util.List;

public interface ProjectService {
    SingleResponse<Integer> createProject(ProjectAddCmd projectAddCmd);

    Response addProjectMember(ProjectMemberAddCmd projectMemberAddCmd);

    SingleResponse<List<ExpenseProjectMemberDTO>> listProjectMember(ProjectMemberListQry projectMemberListQry);

    Response deleteProject(ProjectDeleteCmd projectDeleteCmd);

    PageResponse<ProjectDTO> pageProject(ProjectListQry projectListQry);

    Response addExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd);

    Response deleteExpenseRecord(ExpenseRecordDeleteCmd expenseRecordDeleteCmd);

    Response updateExpenseRecord(ExpenseRecordUpdateCmd expenseRecordUpdateCmd);

    SingleResponse<List<ExpenseRecordDTO>> listRecord(ExpenseRecordQry expenseRecordQry);


    SingleResponse<List<UserSharingDTO>> getProjectSharingDetail(ProjectSharingQry projectSharingQry);
}
