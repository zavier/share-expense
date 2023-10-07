package com.github.zavier.api;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.ProjectDeleteCmd;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.ProjectMemberAddCmd;
import com.github.zavier.dto.data.ProjectDTO;

public interface ProjectService {
    Response createProject(ProjectAddCmd projectAddCmd);

    Response addProjectMember(ProjectMemberAddCmd projectMemberAddCmd);

    Response deleteProject(ProjectDeleteCmd projectDeleteCmd);

    PageResponse<ProjectDTO> pageProject(ProjectListQry projectListQry);
}
