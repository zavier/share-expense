package com.github.zavier.web;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.ProjectDeleteCmd;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.ProjectMemberAddCmd;
import com.github.zavier.dto.data.ProjectDTO;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/delete")
    public Response deleteProject(@RequestBody ProjectDeleteCmd projectDeleteCmd) {
        return projectService.deleteProject(projectDeleteCmd);
    }

    @PostMapping("/addMember")
    private Response addProjectMember(@RequestBody ProjectMemberAddCmd projectMemberAddCmd) {
        return projectService.addProjectMember(projectMemberAddCmd);
    }

    @GetMapping("/list")
    public PageResponse<ProjectDTO> pageProject(ProjectListQry projectListQry) {
        return projectService.pageProject(projectListQry);
    }
}
