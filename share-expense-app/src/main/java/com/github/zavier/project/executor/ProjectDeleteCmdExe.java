package com.github.zavier.project.executor;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;

@Component
public class ProjectDeleteCmdExe {
    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    public Response execute(Integer projectId, Integer operatorId) {
        Assert.notNull(projectId, "项目ID不能为空");
        Assert.notNull(operatorId, "操作人ID不能为空");

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectId);
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
        Assert.isTrue(Objects.equals(projectOpt.get().getCreateUserId(), operatorId), "无权限");

        expenseProjectGateway.delete(projectId);

        return Response.buildSuccess();
    }
}
