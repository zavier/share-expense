package com.github.zavier.project;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.domain.expense.ExpenseProjectMember;
import com.github.zavier.dto.*;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.project.executor.ProjectAddCmdExe;
import com.github.zavier.project.executor.ProjectDeleteCmdExe;
import com.github.zavier.project.executor.ProjectMemberAddCmdExe;
import com.github.zavier.project.executor.query.ProjectListQryExe;
import com.github.zavier.project.executor.query.ProjectMemberListQryExe;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
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

    @Override
    public Response createProject(ProjectAddCmd projectAddCmd) {
        return projectAddCmdExe.execute(projectAddCmd);
    }

    @Override
    public Response addProjectMember(ProjectMemberAddCmd projectMemberAddCmd) {
        return projectMemberAddCmdExe.addProjectMember(projectMemberAddCmd);
    }

    @Override
    public SingleResponse<List<ExpenseProjectMemberDTO>> listProjectMember(ProjectMemberListQry projectMemberListQry) {
        final List<ExpenseProjectMember> execute = projectMemberListQryExe.execute(projectMemberListQry);
        final List<ExpenseProjectMemberDTO> collect = execute.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return SingleResponse.of(collect);
    }

    private ExpenseProjectMemberDTO convertToDTO(ExpenseProjectMember expenseProjectMember){
        final ExpenseProjectMemberDTO memberDTO = new ExpenseProjectMemberDTO();
        memberDTO.setProjectId(expenseProjectMember.getProjectId());
        memberDTO.setUserId(expenseProjectMember.getUserId());
        memberDTO.setUserName(expenseProjectMember.getUserName());
        memberDTO.setWeight(expenseProjectMember.getWeight());
        return memberDTO;
    }

    @Override
    public Response deleteProject(ProjectDeleteCmd projectDeleteCmd) {
        return projectDeleteCmdExe.execute(projectDeleteCmd.getProjectId(), projectDeleteCmd.getOperatorId());
    }

    @Override
    public PageResponse<ProjectDTO> pageProject(ProjectListQry projectListQry) {
        return projectListQryExe.execute(projectListQry);
    }
}
