package com.github.zavier.api;

import com.alibaba.cola.dto.Response;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.ProjectMemberAddCmd;

public interface ProjectService {
    Response createProject(ProjectAddCmd projectAddCmd);

    Response addProjectMember(ProjectMemberAddCmd projectMemberAddCmd);
}
