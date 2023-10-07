package com.github.zavier.project;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.ProjectDeleteCmd;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.ProjectMemberAddCmd;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.project.executor.ProjectAddCmdExe;
import com.github.zavier.project.executor.ProjectDeleteCmdExe;
import com.github.zavier.project.executor.ProjectMemberAddCmdExe;
import com.github.zavier.project.executor.query.ProjectListQryExe;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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

    @Override
    public Response createProject(ProjectAddCmd projectAddCmd) {
        return projectAddCmdExe.execute(projectAddCmd);
    }

    @Override
    public Response addProjectMember(ProjectMemberAddCmd projectMemberAddCmd) {
        return projectMemberAddCmdExe.addProjectMember(projectMemberAddCmd);
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
