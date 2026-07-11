package com.github.zavier.web;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.alibaba.excel.EasyExcel;
import com.github.zavier.dto.*;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.project.ExpenseApplicationService;
import com.github.zavier.project.executor.bo.ExpenseRecordExcelBO;
import com.github.zavier.vo.PageResponseVo;
import com.github.zavier.vo.ResponseVo;
import com.github.zavier.vo.SingleResponseVo;
import com.github.zavier.web.filter.UserHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expense")
public class ExpenseController {

    @Resource
    private ExpenseApplicationService expenseApplicationService;

    @PostMapping("/project/create")
    public ResponseVo createProject(@RequestBody ProjectAddCmd projectAddCmd) {
        projectAddCmd.setCreateUserId(UserHolder.getUser().getUserId());
        projectAddCmd.setCreateUserName(UserHolder.getUser().getUserName());
        final Response response = expenseApplicationService.createProject(projectAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/project/addRecord")
    public ResponseVo saveExpenseRecord(@RequestBody ExpenseRecordAddCmd expenseRecordAddCmd) {
        expenseRecordAddCmd.setOperatorId(UserHolder.getUser().getUserId());
        final Response response = expenseApplicationService.addExpenseRecord(expenseRecordAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/project/deleteRecord")
    public ResponseVo deleteExpenseRecord(@RequestBody ExpenseRecordDeleteCmd expenseRecordDeleteCmd) {
        expenseRecordDeleteCmd.setOperatorId(UserHolder.getUser().getUserId());
        final Response response = expenseApplicationService.deleteExpenseRecord(expenseRecordDeleteCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/project/updateRecord")
    public ResponseVo updateExpenseRecord(@RequestBody ExpenseRecordUpdateCmd expenseRecordUpdateCmd) {
        expenseRecordUpdateCmd.setOperatorId(UserHolder.getUser().getUserId());
        final Response response = expenseApplicationService.updateExpenseRecord(expenseRecordUpdateCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @GetMapping("/project/listRecord")
    public SingleResponseVo<Map<String, List<ExpenseRecordDTO>>> listRecord(ExpenseRecordQry expenseRecordQry) {
        expenseRecordQry.setOperatorId(UserHolder.getUser().getUserId());
        final SingleResponse<List<ExpenseRecordDTO>> listSingleResponse = expenseApplicationService.listRecord(expenseRecordQry);
        Map<String, List<ExpenseRecordDTO>> map = new HashMap<>();
        map.put("rows", listSingleResponse.getData());
        return SingleResponseVo.of(map);
    }


    @PostMapping("/project/delete")
    public ResponseVo deleteProject(@RequestBody ProjectDeleteCmd projectDeleteCmd) {
        projectDeleteCmd.setOperatorId(UserHolder.getUser().getUserId());
        final Response response = expenseApplicationService.deleteProject(projectDeleteCmd.getProjectId(), projectDeleteCmd.getOperatorId());
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/project/addMember")
    public ResponseVo addProjectMember(@RequestBody ProjectMemberAddCmd projectMemberAddCmd) {
        projectMemberAddCmd.setOperatorId(UserHolder.getUser().getUserId());
        final Response response =  expenseApplicationService.addProjectMember(projectMemberAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    // 下拉列表使用
    @GetMapping("/project/listMember")
    public SingleResponseVo<List<ExpenseProjectMemberDTO>> listProjectMember(ProjectMemberListQry projectMemberListQry) {
        projectMemberListQry.setOperatorId(UserHolder.getUser().getUserId());
        final SingleResponse<List<ExpenseProjectMemberDTO>> listSingleResponse = expenseApplicationService.listProjectMember(projectMemberListQry);
        return SingleResponseVo.buildFromSingleResponse(listSingleResponse);
    }

    // 表格组件使用
    @GetMapping("/project/pageMember")
    public SingleResponseVo pageProjectMember(ProjectMemberListQry projectMemberListQry) {
        projectMemberListQry.setOperatorId(UserHolder.getUser().getUserId());
        final SingleResponse<List<ExpenseProjectMemberDTO>> listSingleResponse = expenseApplicationService.listProjectMember(projectMemberListQry);
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
        final PageResponse<ProjectDTO> pageResponse = expenseApplicationService.pageProject(projectListQry);
        return PageResponseVo.buildFromPageResponse(pageResponse);
    }

    @GetMapping("/project/sharing")
    public SingleResponseVo getProjectSharingDetail(ProjectSharingQry projectSharingQry) {
        projectSharingQry.setOperatorId(UserHolder.getUser().getUserId());
        final SingleResponse<List<UserSharingDTO>> projectSharingDetail = expenseApplicationService.getProjectSharingDetail(projectSharingQry);
        if (!projectSharingDetail.isSuccess()) {
            return SingleResponseVo.buildFailure(projectSharingDetail.getErrCode(), projectSharingDetail.getErrMessage());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("rows", projectSharingDetail.getData());
        return SingleResponseVo.of(map);
    }

    @GetMapping("/project/record/export")
    public void exportFeeRecordDetail(@RequestParam Integer projectId, HttpServletResponse response) throws Exception {
        final SingleResponse<List<ExpenseRecordExcelBO>> execute = expenseApplicationService.exportRecords(projectId, UserHolder.getUser().getUserId());
        Assert.isTrue(execute.isSuccess(), "导出异常");
        final List<ExpenseRecordExcelBO> data = execute.getData();
        Assert.isTrue(data != null && !data.isEmpty(), "无费用记录可供导出");

        final String fileName = data.get(0).getProjectName() + "-费用信息.xlsx";

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
