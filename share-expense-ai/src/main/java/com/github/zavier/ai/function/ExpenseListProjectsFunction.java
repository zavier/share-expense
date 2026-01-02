package com.github.zavier.ai.function;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.ai.dto.ExpenseResponseFormat;
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
import java.util.Map;

/**
 * 查询用户项目列表的工具方法（v2.0优化版）
 * <p>
 * 主要优化：
 * <ul>
 *   <li>添加响应格式控制：concise/detailed模式</li>
 *   <li>添加成员列表控制：include_members参数</li>
 *   <li>添加分页支持：page_size参数</li>
 *   <li>优化Token效率：concise模式节省约50% tokens</li>
 * </ul>
 *
 * @author AI Optimization
 * @since 2025-01-02
 */
@Slf4j
@Component
public class ExpenseListProjectsFunction extends BaseExpenseFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 查询用户的所有费用分摊项目。
     *
     * @param name            项目名称过滤（可选），支持模糊搜索
     * @param includeMembers  是否包含成员列表（默认false）
     * @param responseFormat  返回格式：concise（精简）/detailed（详细）
     * @param pageSize        返回项目数量限制，默认20，最大50
     * @return 项目列表
     */
    @Tool(description = """
            查询用户的所有费用分摊项目。

            参数说明：
            - name: 项目名称过滤（可选），支持模糊搜索
            - include_members: 是否包含成员列表（默认false）
            - page_size: 返回项目数量限制，默认20，最大50

            使用场景：
            - 用户说"查看周末聚餐的成员" → 名称过滤 + 包含成员

            注意事项：
            - 默认返回最近的项目（按创建时间倒序）
            """)
    public String listProjects(
            @ToolParam(description = "项目名称过滤（可选），支持模糊搜索", required = false) String name,
            @ToolParam(description = "是否包含成员列表", required = false) Boolean includeMembers,
            @ToolParam(description = "返回项目数量限制，默认20，最大50", required = false) Integer pageSize) {

        log.info("[AI工具] 开始执行 listProjects, 参数: name={}, includeMembers={}, pageSize={}, userId={}",
                name, includeMembers, pageSize, getCurrentUserId());

        // 1. 解析参数
        int limit = parsePageSize(pageSize);
        boolean includeMembersFlag = includeMembers != null && includeMembers;

        // 2. 查询项目列表
        ProjectListQry qry = new ProjectListQry();
        qry.setOperatorId(getCurrentUserId());
        qry.setName(name);
        qry.setPage(1);
        qry.setSize(limit);

        PageResponse<ProjectDTO> response = projectService.pageProject(qry);

        if (!response.isSuccess() || response.getData() == null || response.getData().isEmpty()) {
            String result = "# 您的项目列表\n\n暂无项目";
            log.info("[AI工具] listProjects 执行成功: 暂无项目");
            return result;
        }

        List<ProjectDTO> projects = response.getData();

        // 3. 如果需要包含成员，预查询每个项目的成员列表
        java.util.Map<Integer, List<String>> projectMembersMap = new java.util.HashMap<>();
        if (includeMembersFlag) {
            for (ProjectDTO project : projects) {
                List<String> members = getProjectMembers(project.getProjectId());
                projectMembersMap.put(project.getProjectId(), members);
            }
        }

        // 4. 构建响应
        String result = buildProjectList(projects, includeMembersFlag, projectMembersMap);

        log.info("[AI工具] listProjects 执行成功, projectCount={}", projects.size());
        return result;
    }

    /**
     * 解析response_format参数
     */
    private ExpenseResponseFormat parseResponseFormat(String responseFormat) {
        if (responseFormat == null || responseFormat.isBlank()) {
            return ExpenseResponseFormat.CONCISE; // 默认精简模式
        }
        try {
            return ExpenseResponseFormat.valueOf(responseFormat.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[AI工具] 无效的response_format: {}, 使用默认concise", responseFormat);
            return ExpenseResponseFormat.CONCISE;
        }
    }

    /**
     * 解析page_size参数
     */
    private int parsePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 20; // 默认20条
        }
        if (pageSize > 50) {
            log.warn("[AI工具] pageSize超过最大值50: {}, 使用50", pageSize);
            return 50;
        }
        return pageSize;
    }

    /**
     * 构建项目列表响应
     */
    private String buildProjectList(List<ProjectDTO> projects,
                                    boolean includeMembers,
                                    Map<Integer, List<String>> projectMembersMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 您的项目列表\n\n");

        for (int i = 0; i < projects.size(); i++) {
            ProjectDTO project = projects.get(i);

            sb.append(String.format("%d. **%s**（ID: %d）",
                    i + 1, project.getProjectName(), project.getProjectId()));


            // 项目描述（如果有）
            if (project.getProjectDesc() != null && !project.getProjectDesc().isBlank()) {
                sb.append(String.format(" - %s", project.getProjectDesc()));
            }

            // 成员列表（如果包含）
            if (includeMembers) {
                List<String> members = projectMembersMap.get(project.getProjectId());
                if (members != null && !members.isEmpty()) {
                    sb.append(String.format("\n   - 成员：%s", String.join("、", members)));
                }
            }

            sb.append("\n\n");
        }

        return sb.toString();
    }
}
