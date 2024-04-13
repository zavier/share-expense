package com.github.zavier.project;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.ProjectMemberFee;
import com.github.zavier.dto.*;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.project.executor.*;
import com.github.zavier.project.executor.converter.ProjectConverter;
import com.github.zavier.project.executor.converter.SharingConverter;
import com.github.zavier.project.executor.query.ProjectListQryExe;
import com.github.zavier.project.executor.query.ProjectMemberListQryExe;
import com.github.zavier.project.executor.query.ProjectSharingQryExe;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@CatchAndLog
public class ProjectServiceImpl implements ProjectService {

    @Resource
    private ProjectAddCmdExe projectAddCmdExe;
    @Resource
    private ProjectMemberAddCmdExe projectMemberAddCmdExe;
    @Resource
    private ProjectDeleteCmdExe projectDeleteCmdExe;
    @Resource
    private ProjectListQryExe projectListQryExe;
    @Resource
    private ProjectMemberListQryExe projectMemberListQryExe;
    @Resource
    private ExpenseRecordAddCmdExe expenseRecordAddCmdExe;
    @Resource
    private ExpenseRecordListQryExe expenseRecordListQryExe;
    @Resource
    private ProjectSharingQryExe projectSharingQryExe;

    @Override
    public SingleResponse<Integer> createProject(ProjectAddCmd projectAddCmd) {
        return projectAddCmdExe.execute(projectAddCmd);
    }

    @Override
    public Response addProjectMember(ProjectMemberAddCmd projectMemberAddCmd) {
        return projectMemberAddCmdExe.addProjectMember(projectMemberAddCmd);
    }

    @Override
    public SingleResponse<List<ExpenseProjectMemberDTO>> listProjectMember(ProjectMemberListQry projectMemberListQry) {
        final List<String> members = projectMemberListQryExe.execute(projectMemberListQry);
        final List<ExpenseProjectMemberDTO> collect = members.stream().map(it -> {
            final ExpenseProjectMemberDTO expenseProjectMemberDTO = new ExpenseProjectMemberDTO();
            expenseProjectMemberDTO.setMember(it);
            expenseProjectMemberDTO.setProjectId(projectMemberListQry.getProjectId());
            return expenseProjectMemberDTO;
        }).collect(Collectors.toList());
        return SingleResponse.of(collect);
    }

    @Override
    public Response deleteProject(ProjectDeleteCmd projectDeleteCmd) {
        return projectDeleteCmdExe.execute(projectDeleteCmd.getProjectId(), projectDeleteCmd.getOperatorId());
    }

    @Override
    public PageResponse<ProjectDTO> pageProject(ProjectListQry projectListQry) {
        return projectListQryExe.execute(projectListQry);
    }

    @Override
    public Response addExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd) {
        expenseRecordAddCmdExe.execute(expenseRecordAddCmd);
        return Response.buildSuccess();
    }

    @Override
    public SingleResponse<List<ExpenseRecordDTO>> listRecord(ExpenseRecordQry expenseRecordQry) {
        final SingleResponse<List<ExpenseRecord>> response = expenseRecordListQryExe.execute(expenseRecordQry);
        if (!response.isSuccess()) {
            return SingleResponse.buildFailure(response.getErrCode(), response.getErrMessage());
        }
        final List<ExpenseRecord> data = response.getData();
        if (CollectionUtils.isEmpty(data)) {
            return SingleResponse.of(Collections.emptyList());
        }

        final List<ExpenseRecordDTO> collect = data.stream()
                .map(ProjectConverter::convertToDTO)
                .collect(Collectors.toList());

        return SingleResponse.of(collect);

    }


    @Override
    public SingleResponse<List<UserSharingDTO>> getProjectSharingDetail(ProjectSharingQry projectSharingQry) {
        final ProjectMemberFee execute = projectSharingQryExe.execute(projectSharingQry);

        final List<UserSharingDTO> sharingDTOList = SharingConverter.convert(execute);
        return SingleResponse.of(sharingDTOList);
    }
}
