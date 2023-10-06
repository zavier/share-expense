package com.github.zavier.web;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.ProjectMemberAddCmd;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/project")
public class ProjectController {

    @Resource
    private ProjectService projectService;

    @PostMapping("/create")
    public Response createProject(@RequestBody ProjectAddCmd projectAddCmd) {
        return projectService.createProject(projectAddCmd);
    }

    @PostMapping("/addMember")
    private Response addProjectMember(@RequestBody ProjectMemberAddCmd projectMemberAddCmd) {
        return projectService.addProjectMember(projectMemberAddCmd);
    }
}
