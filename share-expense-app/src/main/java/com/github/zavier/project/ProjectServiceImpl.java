package com.github.zavier.project;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.ProjectMemberAddCmd;
import com.github.zavier.project.executor.ProjectAddCmdExe;
import com.github.zavier.project.executor.ProjectMemberAddCmdExe;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@CatchAndLog
public class ProjectServiceImpl implements ProjectService {

    @Resource
    private ProjectAddCmdExe projectAddCmdExe;
    @Resource
    private ProjectMemberAddCmdExe projectMemberAddCmdExe;

    @Override
    public Response createProject(ProjectAddCmd projectAddCmd) {
        return projectAddCmdExe.execute(projectAddCmd);
    }

    @Override
    public Response addProjectMember(ProjectMemberAddCmd projectMemberAddCmd) {
        return projectMemberAddCmdExe.addProjectMember(projectMemberAddCmd);
    }
}
