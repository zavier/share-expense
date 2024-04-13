package com.github.zavier.web;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.alibaba.excel.EasyExcel;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.*;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.project.executor.ExpenseRecordExportExe;
import com.github.zavier.project.executor.bo.ExpenseRecordExcelBO;
import com.github.zavier.vo.PageResponseVo;
import com.github.zavier.vo.ResponseVo;
import com.github.zavier.vo.SingleResponseVo;
import com.github.zavier.web.filter.UserHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expense")
public class ExpenseController {

    @Resource
    private ProjectService projectService;

    @Resource
    private ExpenseRecordExportExe expenseRecordExportExe;

    @PostMapping("/project/create")
    public ResponseVo createProject(@RequestBody ProjectAddCmd projectAddCmd) {
        projectAddCmd.setCreateUserId(UserHolder.getUser().getUserId());
        projectAddCmd.setCreateUserName(UserHolder.getUser().getUserName());
        final Response response = projectService.createProject(projectAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/project/addRecord")
    public ResponseVo saveExpenseRecord(@RequestBody ExpenseRecordAddCmd expenseRecordAddCmd) {
        expenseRecordAddCmd.setOperatorId(UserHolder.getUser().getUserId());
        final Response response = projectService.addExpenseRecord(expenseRecordAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @GetMapping("/project/listRecord")
    public SingleResponseVo<Map<String, List<ExpenseRecordDTO>>> listRecord(ExpenseRecordQry expenseRecordQry) {
        expenseRecordQry.setOperatorId(UserHolder.getUser().getUserId());
        final SingleResponse<List<ExpenseRecordDTO>> listSingleResponse = projectService.listRecord(expenseRecordQry);
        Map<String, List<ExpenseRecordDTO>> map = new HashMap<>();
        map.put("rows", listSingleResponse.getData());
        return SingleResponseVo.of(map);
    }


    @PostMapping("/project/delete")
    public ResponseVo deleteProject(@RequestBody ProjectDeleteCmd projectDeleteCmd) {
        projectDeleteCmd.setOperatorId(UserHolder.getUser().getUserId());
        final Response response = projectService.deleteProject(projectDeleteCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/project/addMember")
    private ResponseVo addProjectMember(@RequestBody ProjectMemberAddCmd projectMemberAddCmd) {
        projectMemberAddCmd.setOperatorId(UserHolder.getUser().getUserId());
        final Response response =  projectService.addProjectMember(projectMemberAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    // 下拉列表使用
    @GetMapping("/project/listMember")
    private SingleResponseVo<List<ExpenseProjectMemberDTO>> listProjectMember(ProjectMemberListQry projectMemberListQry) {
        projectMemberListQry.setOperatorId(UserHolder.getUser().getUserId());
        final SingleResponse<List<ExpenseProjectMemberDTO>> listSingleResponse = projectService.listProjectMember(projectMemberListQry);
        return SingleResponseVo.buildFromSingleResponse(listSingleResponse);
    }

    // 表格组件使用
    @GetMapping("/project/pageMember")
    private SingleResponseVo pageProjectMember(ProjectMemberListQry projectMemberListQry) {
        projectMemberListQry.setOperatorId(UserHolder.getUser().getUserId());
        final SingleResponse<List<ExpenseProjectMemberDTO>> listSingleResponse = projectService.listProjectMember(projectMemberListQry);
        final List<ExpenseProjectMemberDTO> data = listSingleResponse.getData();
        Map<String, Object> map = new HashMap<>();
        map.put("count", data.size());
        map.put("rows", data);
        return SingleResponseVo.of(map);
    }

    @GetMapping("/project/list")
    public PageResponseVo<ProjectDTO> pageProject(ProjectListQry projectListQry) {
        final int userId = UserHolder.getUser().getUserId();
        projectListQry.setOperatorId(userId);
        final PageResponse<ProjectDTO> pageResponse = projectService.pageProject(projectListQry);
        return PageResponseVo.buildFromPageResponse(pageResponse);
    }

    @GetMapping("/project/sharing")
    public SingleResponseVo getProjectSharingDetail(ProjectSharingQry projectSharingQry) {
        projectSharingQry.setOperatorId(UserHolder.getUser().getUserId());
        final SingleResponse<List<UserSharingDTO>> projectSharingDetail = projectService.getProjectSharingDetail(projectSharingQry);
        if (!projectSharingDetail.isSuccess()) {
            return SingleResponseVo.buildFailure(projectSharingDetail.getErrCode(), projectSharingDetail.getErrMessage());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("rows", projectSharingDetail.getData());
        return SingleResponseVo.of(map);
    }

    @GetMapping("/project/record/export")
    public void exportFeeRecordDetail(@RequestParam Integer projectId, HttpServletResponse response) throws Exception {
        // TODO expenseRecordExportExe 后续看看迁移到 projectService中使用？ 同时也可以使用到注解切面
        // 这里因为导出类使用了easyexcel的注解，所以临时这样处理吧
        final SingleResponse<List<ExpenseRecordExcelBO>> execute = expenseRecordExportExe.execute(projectId, UserHolder.getUser().getUserId());
        Assert.isTrue(execute.isSuccess(), "导出异常");

        final String fileName = execute.getData().get(0).getProjectName() + "-费用信息.xlsx";

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        final String header = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, header);

        final ServletOutputStream outputStream = response.getOutputStream();
        EasyExcel.write(outputStream, ExpenseRecordExcelBO.class)
                .sheet("费用记录")
                .doWrite(execute::getData);
        outputStream.flush();
        outputStream.close();
    }

}
