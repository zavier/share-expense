package com.github.zavier.ai.function;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectAddCmd;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AiFunction(
    name = "createProject",
    description = "创建一个新的费用分摊项目。需要提供项目名称和成员列表。"
)
public class CreateProjectFunction implements AiFunctionExecutor {

    @Resource
    private ProjectService projectService;

    public record Request(
        String projectName,
        String description,
        List<String> members
    ) {}

    @Override
    public String execute(Object request, FunctionContext context) {
        Request req = (Request) request;
        ProjectAddCmd cmd = new ProjectAddCmd();
        cmd.setProjectName(req.projectName());
        cmd.setProjectDesc(req.description());
        cmd.setCreateUserId(context.getUserId());
        cmd.setCreateUserName("AI用户");  // TODO: 从用户信息获取
        cmd.setMembers(req.members());

        SingleResponse<Integer> response = projectService.createProject(cmd);

        if (!response.isSuccess()) {
            throw new RuntimeException("创建项目失败: " + response.getErrMessage());
        }

        Integer projectId = response.getData();
        return String.format("项目创建成功！项目名称：%s，项目ID：%d", req.projectName(), projectId);
    }

    @Override
    public Class<Request> getRequestType() {
        return Request.class;
    }
}
