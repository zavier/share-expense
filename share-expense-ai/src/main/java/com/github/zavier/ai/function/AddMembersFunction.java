package com.github.zavier.ai.function;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectMemberAddCmd;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AiFunction(
    name = "addMembers",
    description = "向现有项目添加新成员。需要提供项目ID和成员名称列表。"
)
public class AddMembersFunction implements AiFunctionExecutor {

    @Resource
    private ProjectService projectService;

    public record Request(
        Integer projectId,
        List<String> members
    ) {}

    @Override
    public String execute(Object request, FunctionContext context) {
        Request req = (Request) request;
        ProjectMemberAddCmd cmd = new ProjectMemberAddCmd();
        cmd.setProjectId(req.projectId());
        cmd.setMembers(req.members());
        cmd.setOperatorId(context.getUserId());

        Response response = projectService.addProjectMember(cmd);

        if (!response.isSuccess()) {
            throw new RuntimeException("添加成员失败: " + response.getErrMessage());
        }

        return String.format("成功添加 %d 个成员到项目 %d", req.members().size(), req.projectId());
    }

    @Override
    public Class<Request> getRequestType() {
        return Request.class;
    }
}
