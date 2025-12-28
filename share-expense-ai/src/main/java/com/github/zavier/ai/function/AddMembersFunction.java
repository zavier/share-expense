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
 * 向项目添加成员的工具方法
 */
@Slf4j
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

        log.info("[AI工具] 开始执行 addMembers, 参数: projectId={}, members={}, userId={}",
            projectId, members, getCurrentUserId());

        ProjectMemberAddCmd cmd = new ProjectMemberAddCmd();
        cmd.setProjectId(projectId);
        cmd.setMembers(members);
        cmd.setOperatorId(getCurrentUserId());

        Response response = projectService.addProjectMember(cmd);

        if (!response.isSuccess()) {
            log.error("[AI工具] addMembers 执行失败: {}", response.getErrMessage());
            return "添加成员失败: " + response.getErrMessage();
        }

        String result = String.format("成功添加 %d 个成员到项目 %d", members.size(), projectId);
        log.info("[AI工具] addMembers 执行成功: {}", result);
        return result;
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
