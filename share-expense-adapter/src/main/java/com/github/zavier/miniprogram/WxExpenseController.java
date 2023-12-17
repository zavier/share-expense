package com.github.zavier.miniprogram;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.*;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.vo.PageResponseVo;
import com.github.zavier.vo.ResponseVo;
import com.github.zavier.vo.SingleResponseVo;
import com.github.zavier.web.filter.UserHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wx/expense")
public class WxExpenseController {

    private final ProjectService projectService;

    public WxExpenseController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/project/addRecord")
    public ResponseVo saveExpenseRecord(@RequestBody ExpenseRecordAddCmd expenseRecordAddCmd) {
        final Response response = projectService.addExpenseRecord(expenseRecordAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @GetMapping("/project/listRecord")
    public SingleResponseVo<Map<String, List<ExpenseRecordDTO>>> listRecord(ExpenseRecordQry expenseRecordQry) {
        final SingleResponse<List<ExpenseRecordDTO>> listSingleResponse = projectService.listRecord(expenseRecordQry);
        Map<String, List<ExpenseRecordDTO>> map = new HashMap<>();
        map.put("rows", listSingleResponse.getData());
        return SingleResponseVo.of(map);
    }


    @PostMapping("/project/create")
    public ResponseVo createProject(@RequestBody ProjectAddCmd projectAddCmd) {
        projectAddCmd.setUserId(UserHolder.getUser().getUserId());
        projectAddCmd.setUserName(UserHolder.getUser().getUserName());
        final Response response = projectService.createProject(projectAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/project/delete")
    public ResponseVo deleteProject(@RequestBody ProjectDeleteCmd projectDeleteCmd) {
        projectDeleteCmd.setOperatorId(UserHolder.getUser().getUserId());
        final Response response = projectService.deleteProject(projectDeleteCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/project/addMember")
    private ResponseVo addProjectMember(@RequestBody ProjectMemberAddCmd projectMemberAddCmd) {
        final Response response =  projectService.addProjectMember(projectMemberAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @GetMapping("/project/listMember")
    private SingleResponseVo<List<ExpenseProjectMemberDTO>> listProjectMember(ProjectMemberListQry projectMemberListQry) {
        final SingleResponse<List<ExpenseProjectMemberDTO>> listSingleResponse = projectService.listProjectMember(projectMemberListQry);
        return SingleResponseVo.buildFromSingleResponse(listSingleResponse);
    }

    @GetMapping("/project/pageMember")
    private SingleResponseVo pageProjectMember(ProjectMemberListQry projectMemberListQry) {
        final SingleResponse<List<ExpenseProjectMemberDTO>> listSingleResponse = projectService.listProjectMember(projectMemberListQry);
        final List<ExpenseProjectMemberDTO> data = listSingleResponse.getData();
        Map<String, Object> map = new HashMap<>();
        map.put("count", data.size());
        map.put("rows", data);
        return SingleResponseVo.of(map);
    }

    @GetMapping("/project/list")
    public PageResponseVo<ProjectDTO> pageProject(ProjectListQry projectListQry) {
        final int userId = 1;
        projectListQry.setUserId(userId);
        final PageResponse<ProjectDTO> pageResponse = projectService.pageProject(projectListQry);
        return PageResponseVo.buildFromPageResponse(pageResponse);
    }

    @GetMapping("/project/sharing")
    public SingleResponseVo getProjectSharingDetail(ProjectSharingQry projectSharingQry) {
        final SingleResponse<List<UserSharingDTO>> projectSharingDetail = projectService.getProjectSharingDetail(projectSharingQry);
        if (!projectSharingDetail.isSuccess()) {
            return SingleResponseVo.buildFailure(projectSharingDetail.getErrCode(), projectSharingDetail.getErrMessage());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("rows", projectSharingDetail.getData());
        return SingleResponseVo.of(map);
    }

}
