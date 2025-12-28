package com.github.zavier.ai.function;

import com.alibaba.cola.dto.PageResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 查询用户项目列表的工具方法
 */
@Slf4j
@Component
public class ListProjectsFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 查询用户的项目列表
     *
     * @param name 项目名称过滤（可选，支持模糊搜索）
     * @return 项目列表文本描述
     */
    @Tool(description = "查询用户的所有费用分摊项目，返回项目ID和名称列表。如果用户指定了项目名称，可以进行模糊搜索。")
    public String listProjects(
            @ToolParam(description = "项目名称（可选，用于过滤）", required = false) String name) {

        log.info("[AI工具] 开始执行 listProjects, 参数: name={}, userId={}", name, getCurrentUserId());

        ProjectListQry qry = new ProjectListQry();
        qry.setOperatorId(getCurrentUserId());
        qry.setName(name);
        qry.setPage(1);
        qry.setSize(50);  // 默认返回50个项目，足够大多数用户

        PageResponse<ProjectDTO> response = projectService.pageProject(qry);

        if (!response.isSuccess()) {
            log.error("[AI工具] listProjects 执行失败: {}", response.getErrMessage());
            return "查询项目列表失败: " + response.getErrMessage();
        }

        if (response.getData().isEmpty()) {
            String result = "您还没有任何项目";
            log.info("[AI工具] listProjects 执行成功: 暂无项目");
            return result;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("您的项目列表：\n");

        for (ProjectDTO project : response.getData()) {
            sb.append(String.format("- 项目ID：%d，项目名称：%s", project.getProjectId(), project.getProjectName()));
            if (project.getProjectDesc() != null && !project.getProjectDesc().isBlank()) {
                sb.append(String.format("（%s）", project.getProjectDesc()));
            }
            sb.append("\n");
        }

        String result = sb.toString();
        log.info("[AI工具] listProjects 执行成功, 项目数={}", response.getData().size());
        return result;
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
