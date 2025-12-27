package com.github.zavier.ai.function;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectMemberAddCmd;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向项目添加成员的工具方法
 */
@Component
public class AddMembersFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 向现有项目添加新成员
     *
     * @param projectId 项目ID
     * @param members 成员名称列表
     * @return 添加结果消息
     */
    @Tool(description = "向现有项目添加新成员。需要提供项目ID和成员名称列表。")
    public String addMembers(
            @ToolParam(description = "项目ID") Integer projectId,
            @ToolParam(description = "成员名称列表") List<String> members) {

        ProjectMemberAddCmd cmd = new ProjectMemberAddCmd();
        cmd.setProjectId(projectId);
        cmd.setMembers(members);
        cmd.setOperatorId(getCurrentUserId());

        Response response = projectService.addProjectMember(cmd);

        if (!response.isSuccess()) {
            return "添加成员失败: " + response.getErrMessage();
        }

        return String.format("成功添加 %d 个成员到项目 %d", members.size(), projectId);
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
