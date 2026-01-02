package com.github.zavier.ai.function;

import com.github.zavier.ai.resolver.ProjectIdentifierResolver;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectMemberListQry;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI工具函数基类
 * <p>
 * 提供公共方法和工具函数，减少代码重复。
 * 所有费用分摊相关的AI工具函数都应继承此类。
 *
 * @author AI Optimization
 * @since 2025-01-02
 */
@Slf4j
public abstract class BaseExpenseFunction {

    @Resource
    private ProjectIdentifierResolver projectIdentifierResolver;

    @Resource
    private ProjectService projectService;

    /**
     * 解析项目标识符（ID或名称）
     * <p>
     * 智能识别参数是项目ID还是项目名称，统一返回项目ID。
     *
     * @param projectIdentifier 项目标识符（名称或ID）
     * @return 项目ID，如果未找到返回null
     */
    protected Integer resolveProjectIdentifier(String projectIdentifier) {
        if (projectIdentifier == null || projectIdentifier.isBlank()) {
            log.warn("[项目标识符解析] 标识符为空");
            return null;
        }
        return projectIdentifierResolver.resolve(projectIdentifier, getCurrentUserId());
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果未登录返回默认值1
     */
    protected Integer getCurrentUserId() {
        Assert.notNull(UserHolder.getUser(), "用户未登录");
        return UserHolder.getUser().getUserId();
    }

    /**
     * 获取项目成员列表
     *
     * @param projectId 项目ID
     * @return 成员名称列表
     */
    protected List<String> getProjectMembers(Integer projectId) {
        ProjectMemberListQry qry = new ProjectMemberListQry();
        qry.setProjectId(projectId);
        qry.setOperatorId(getCurrentUserId());

        var response = projectService.listProjectMember(qry);
        if (response.isSuccess() && response.getData() != null) {
            return response.getData().stream()
                    .map(ExpenseProjectMemberDTO::getMember)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * 构建项目未找到的错误响应
     *
     * @param identifier 项目标识符
     * @return 错误消息
     */
    protected String buildProjectNotFoundResponse(String identifier) {
        return String.format("""
                ❌ 未找到项目"%s"

                建议：
                1. 使用 expense_list_projects 查看所有项目
                2. 检查项目名称是否正确
                3. 可以使用项目ID（数字）代替项目名称
                """, identifier);
    }

    /**
     * 构建成员未找到的错误响应
     *
     * @param role          角色（如"付款人"、"消费成员"）
     * @param memberName    成员名称
     * @param validMembers  有效成员列表
     * @return 错误消息
     */
    protected String buildMemberNotFoundResponse(String role, String memberName, List<String> validMembers) {
        return String.format("""
                ❌ %s"%s"不在项目成员列表中

                当前项目成员：%s

                建议：
                1. 检查成员姓名是否正确
                2. 使用 expense_add_members 添加新成员到项目
                """, role, memberName, String.join("、", validMembers));
    }

    /**
     * 构建参数缺失的错误响应
     *
     * @param paramName 参数名称
     * @return 错误消息
     */
    protected String buildMissingParamResponse(String paramName) {
        return String.format("❌ 缺少必要参数：%s\n\n请提供该参数后重试。", paramName);
    }

    /**
     * 构建参数格式错误的错误响应
     *
     * @param paramName 参数名称
     * @param expectedFormat 期望的格式
     * @return 错误消息
     */
    protected String buildInvalidParamFormatResponse(String paramName, String expectedFormat) {
        return String.format("❌ 参数格式错误：%s\n\n正确格式：%s", paramName, expectedFormat);
    }
}
