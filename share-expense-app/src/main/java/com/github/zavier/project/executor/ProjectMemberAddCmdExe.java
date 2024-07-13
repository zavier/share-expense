package com.github.zavier.project.executor;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectMemberAddCmd;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class ProjectMemberAddCmdExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    public Response addProjectMember(ProjectMemberAddCmd projectMemberAddCmd) {
        Assert.notNull(projectMemberAddCmd.getProjectId(), "项目ID不能为空");
        Assert.notEmpty(projectMemberAddCmd.getMembers(), "成员信息不能为空");

        final List<String> userNameList = projectMemberAddCmd.getMembers();

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectMemberAddCmd.getProjectId());
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOpt.get();

        Assert.isTrue(Objects.equals(expenseProject.getCreateUserId(), projectMemberAddCmd.getOperatorId()), "无权限");

        userNameList.forEach(expenseProject::addMember);
        expenseProject.setChangingStatus(ChangingStatus.UPDATED);
        expenseProjectGateway.save(expenseProject);
        return Response.buildSuccess();
    }
}
