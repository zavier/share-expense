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
 * 创建费用分摊项目的工具方法
 */
@Slf4j
@Component
public class CreateProjectFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 创建一个新的费用分摊项目
     *
     * @param projectName 项目名称
     * @param description 项目描述（可选）
     * @param members 成员名称列表
     * @return 创建结果消息
     */
    @Tool(description = "创建一个新的费用分摊项目。需要提供项目名称和成员列表。")
    public String createProject(
            @ToolParam(description = "项目名称") String projectName,
            @ToolParam(description = "项目描述", required = false) String description,
            @ToolParam(description = "成员名称列表") List<String> members) {

        log.info("[AI工具] 开始执行 createProject, 参数: projectName={}, description={}, members={}, userId={}",
            projectName, description, members, getCurrentUserId());

        ProjectAddCmd cmd = new ProjectAddCmd();
        cmd.setProjectName(projectName);
        cmd.setProjectDesc(description);
        cmd.setCreateUserId(getCurrentUserId());
        cmd.setCreateUserName("AI用户");
        cmd.setMembers(members);

        SingleResponse<Integer> response = projectService.createProject(cmd);

        if (!response.isSuccess()) {
            log.error("[AI工具] createProject 执行失败: {}", response.getErrMessage());
            return "创建项目失败: " + response.getErrMessage();
        }

        Integer projectId = response.getData();
        String result = String.format("项目创建成功！项目名称：%s，项目ID：%d", projectName, projectId);
        log.info("[AI工具] createProject 执行成功: {}", result);
        return result;
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
