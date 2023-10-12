package com.github.zavier.web;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.ProjectDeleteCmd;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.ProjectMemberAddCmd;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.vo.PageResponseVo;
import com.github.zavier.vo.ResponseVo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/project")
public class ProjectController {

    @Resource
    private ProjectService projectService;

    @PostMapping("/create")
    public ResponseVo createProject(@RequestBody ProjectAddCmd projectAddCmd) {
        final Response response = projectService.createProject(projectAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/delete")
    public ResponseVo deleteProject(@RequestBody ProjectDeleteCmd projectDeleteCmd) {
        final Response response = projectService.deleteProject(projectDeleteCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/addMember")
    private ResponseVo addProjectMember(@RequestBody ProjectMemberAddCmd projectMemberAddCmd) {
        final Response response =  projectService.addProjectMember(projectMemberAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @GetMapping("/list")
    public PageResponseVo<ProjectDTO> pageProject(ProjectListQry projectListQry) {
        final PageResponse<ProjectDTO> pageResponse = projectService.pageProject(projectListQry);
        return PageResponseVo.buildFromPageResponse(pageResponse);
    }
}
