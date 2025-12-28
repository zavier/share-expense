package com.github.zavier.ai.function;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.ProjectMemberListQry;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询项目详情的工具方法
 */
@Slf4j
@Component
public class GetProjectDetailsFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 根据项目ID查询项目的详细信息
     *
     * @param projectId 项目ID
     * @return 项目详情文本描述
     */
    @Tool(description = "根据项目ID查询项目的详细信息，包括项目名称、描述和成员列表")
    public String getProjectDetails(@ToolParam(description = "项目ID") Integer projectId) {

        log.info("[AI工具] 开始执行 getProjectDetails, 参数: projectId={}, userId={}",
            projectId, getCurrentUserId());

        // 查询项目基本信息
        ProjectListQry projectQry = new ProjectListQry();
        projectQry.setId(projectId);
        projectQry.setOperatorId(getCurrentUserId());
        projectQry.setPage(1);
        projectQry.setSize(1);

        PageResponse<ProjectDTO> projectResponse = projectService.pageProject(projectQry);

        if (!projectResponse.isSuccess() || projectResponse.getData().isEmpty()) {
            log.warn("[AI工具] getProjectDetails 未找到项目: projectId={}", projectId);
            return "未找到项目ID为 " + projectId + " 的项目";
        }

        ProjectDTO project = projectResponse.getData().get(0);

        // 查询项目成员列表
        ProjectMemberListQry memberQry = new ProjectMemberListQry();
        memberQry.setProjectId(projectId);
        memberQry.setOperatorId(getCurrentUserId());

        SingleResponse<List<ExpenseProjectMemberDTO>> memberResponse = projectService.listProjectMember(memberQry);

        List<ExpenseProjectMemberDTO> members = List.of();
        if (memberResponse.isSuccess() && memberResponse.getData() != null) {
            members = memberResponse.getData();
        }

        // 组装返回结果
        return buildProjectDetailsText(project, members);
    }

    /**
     * 构建项目详情文本
     */
    private String buildProjectDetailsText(ProjectDTO project, List<ExpenseProjectMemberDTO> members) {
        StringBuilder sb = new StringBuilder();
        sb.append("项目详情：\n");
        sb.append(String.format("- 项目ID：%d\n", project.getProjectId()));
        sb.append(String.format("- 项目名称：%s\n", project.getProjectName()));

        if (project.getProjectDesc() != null && !project.getProjectDesc().isBlank()) {
            sb.append(String.format("- 项目描述：%s\n", project.getProjectDesc()));
        }

        if (members.isEmpty()) {
            sb.append("- 成员列表：暂无成员");
        } else {
            sb.append(String.format("- 成员列表（%d人）：", members.size()));
            for (int i = 0; i < members.size(); i++) {
                if (i > 0) {
                    sb.append("、");
                }
                sb.append(members.get(i).getMember());
            }
        }

        String result = sb.toString();
        log.info("[AI工具] getProjectDetails 执行成功: {}", result);
        return result;
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
