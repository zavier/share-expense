package com.github.zavier.ai.function;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 创建费用分摊项目的工具方法（v2.0优化版）
 * <p>
 * 主要优化：
 * <ul>
 *   <li>增强工具描述：详细的使用场景和注意事项</li>
 *   <li>增强参数验证：成员列表不能为空</li>
 *   <li>增强错误提示：提供具体的错误原因</li>
 * </ul>
 *
 * @author AI Optimization
 * @since 2025-01-02
 */
@Slf4j
@Component
public class ExpenseCreateProjectFunction extends BaseExpenseFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 创建一个新的费用分摊项目。
     *
     * @param projectName 项目名称
     * @param description 项目描述（可选）
     * @param members     成员名称列表
     * @return 创建结果
     */
    @Tool(description = """
            创建一个新的费用分摊项目。

            参数说明：
            - project_name: 项目名称，不能为空
            - description: 项目描述（可选），用于说明项目的具体内容
            - members: 成员名称列表，至少包含1个成员

            使用场景：
            - 用户说"创建一个周末聚餐项目，成员有Alice、Bob、Charlie"
            - 用户说"新建项目叫公司团建，包含5个人：张三、李四、王五、赵六、钱七"
            - 用户说"建个账单分摊群，我和室友一起用"

            注意事项：
            - 项目名称不能为空
            - 成员列表至少包含1人
            - 创建后可以通过expense_add_members添加更多成员
            - 创建后可以通过expense_add_expense添加费用记录

            错误处理：
            - 如果项目名称为空，会提示必须提供项目名称
            - 如果成员列表为空，会提示必须提供成员
            """)
    public String createProject(
            @ToolParam(description = "项目名称，不能为空") String projectName,
            @ToolParam(description = "项目描述（可选）", required = false) String description,
            @ToolParam(description = "成员名称列表，至少包含1个成员") List<String> members) {

        log.info("[AI工具] 开始执行 createProject, 参数: projectName={}, description={}, members={}, userId={}",
                projectName, description, members, getCurrentUserId());

        // 1. 验证项目名称
        if (projectName == null || projectName.isBlank()) {
            log.warn("[AI工具] createProject 项目名称为空");
            return buildMissingParamResponse("project_name（项目名称）");
        }

        // 2. 验证成员列表
        if (members == null || members.isEmpty()) {
            log.warn("[AI工具] createProject 成员列表为空");
            return buildMissingParamResponse("members（成员列表）");
        }

        // 过滤掉空字符串
        List<String> validMembers = members.stream()
                .filter(member -> member != null && !member.isBlank())
                .toList();

        if (validMembers.isEmpty()) {
            log.warn("[AI工具] createProject 成员列表全部为空");
            return "❌ 成员列表不能为空，请提供至少1个成员名称";
        }

        // 3. 构建命令对象
        ProjectAddCmd cmd = new ProjectAddCmd();
        cmd.setProjectName(projectName);
        cmd.setProjectDesc(description);
        cmd.setCreateUserId(getCurrentUserId());
        cmd.setCreateUserName("AI用户");
        cmd.setMembers(validMembers);

        // 4. 调用业务逻辑
        SingleResponse<Integer> response = projectService.createProject(cmd);

        if (!response.isSuccess()) {
            log.error("[AI工具] createProject 执行失败: {}", response.getErrMessage());
            return "❌ 创建项目失败: " + response.getErrMessage();
        }

        Integer projectId = response.getData();
        String result = String.format("✅ 项目创建成功！\n\n- 项目名称：%s\n- 项目ID：%d\n- 成员数量：%d 人\n- 成员列表：%s",
                projectName, projectId, validMembers.size(), String.join("、", validMembers));
        log.info("[AI工具] createProject 执行成功: {}", result);
        return result;
    }
}
