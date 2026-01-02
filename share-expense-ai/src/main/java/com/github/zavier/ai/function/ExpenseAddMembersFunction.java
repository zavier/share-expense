package com.github.zavier.ai.function;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectMemberAddCmd;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向项目添加成员的工具方法（v2.0优化版）
 * <p>
 * 主要优化：
 * <ul>
 *   <li>支持项目名称或ID自动识别</li>
 *   <li>增强工具描述：详细的使用场景</li>
 *   <li>增强错误提示：提供具体的错误原因</li>
 * </ul>
 *
 * @author AI Optimization
 * @since 2025-01-02
 */
@Slf4j
@Component
public class ExpenseAddMembersFunction extends BaseExpenseFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 向现有项目添加新成员。
     *
     * @param projectIdentifier 项目名称或项目ID
     * @param members           成员名称列表
     * @return 添加结果
     */
    @Tool(description = """
            向现有项目添加新成员。

            参数说明：
            - project_identifier: 项目名称或项目ID（如"周末聚餐"或"5"）
            - members: 成员名称列表，至少包含1个成员

            使用场景：
            - 用户说"给周末聚餐项目添加成员David"
            - 用户说"添加Alice和Bob到项目5"
            - 用户说"周末聚餐再加3个人：Charlie、David、Eve"

            注意事项：
            - 成员名称不能为空
            - 如果成员已存在于项目中，会被忽略
            - 一次可以添加多个成员

            错误处理：
            - 如果项目不存在，会返回明确的错误提示
            - 如果成员列表为空，会提示必须提供成员
            """)
    public String addMembers(
            @ToolParam(description = "项目名称或项目ID") String projectIdentifier,
            @ToolParam(description = "成员名称列表，至少包含1个成员") List<String> members) {

        log.info("[AI工具] 开始执行 addMembers, 参数: projectIdentifier={}, members={}, userId={}",
                projectIdentifier, members, getCurrentUserId());

        // 1. 解析项目标识符
        Integer projectId = resolveProjectIdentifier(projectIdentifier);
        if (projectId == null) {
            log.warn("[AI工具] addMembers 未找到项目: projectIdentifier={}", projectIdentifier);
            return buildProjectNotFoundResponse(projectIdentifier);
        }

        // 2. 验证成员列表
        if (members == null || members.isEmpty()) {
            log.warn("[AI工具] addMembers 成员列表为空");
            return buildMissingParamResponse("members（成员列表）");
        }

        // 过滤掉空字符串
        List<String> validMembers = members.stream()
                .filter(member -> member != null && !member.isBlank())
                .toList();

        if (validMembers.isEmpty()) {
            log.warn("[AI工具] addMembers 成员列表全部为空");
            return "❌ 成员列表不能为空，请提供至少1个成员名称";
        }

        // 3. 构建命令对象
        ProjectMemberAddCmd cmd = new ProjectMemberAddCmd();
        cmd.setProjectId(projectId);
        cmd.setMembers(validMembers);
        cmd.setOperatorId(getCurrentUserId());

        // 4. 调用业务逻辑
        Response response = projectService.addProjectMember(cmd);

        if (!response.isSuccess()) {
            log.error("[AI工具] addMembers 执行失败: {}", response.getErrMessage());
            return "❌ 添加成员失败: " + response.getErrMessage();
        }

        String result = String.format("✅ 成功添加 %d 个成员到项目\"%s\"\n\n- 新成员：%s",
                validMembers.size(), projectIdentifier, String.join("、", validMembers));
        log.info("[AI工具] addMembers 执行成功: {}", result);
        return result;
    }
}
