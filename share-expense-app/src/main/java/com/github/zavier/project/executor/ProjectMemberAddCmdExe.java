package com.github.zavier.project.executor;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.ProjectMemberAddCmd;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Component
public class ProjectMemberAddCmdExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;
    @Resource
    private UserGateway userGateway;

    public Response addProjectVirtualMember(ProjectMemberAddCmd projectMemberAddCmd) {
        Assert.notNull(projectMemberAddCmd.getProjectId(), "项目ID不能为空");
        Assert.isTrue(StringUtils.isNoneBlank(projectMemberAddCmd.getUserNames()), "成员信息不能为空");

        final List<String> userNameList =
                Splitter.on(",").trimResults().trimResults().splitToList(projectMemberAddCmd.getUserNames());

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectMemberAddCmd.getProjectId());
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOpt.get();
        userNameList.forEach(expenseProject::addVirtualMember);
        expenseProject.setChangingStatus(ChangingStatus.UPDATED);
        expenseProjectGateway.save(expenseProject);
        return Response.buildSuccess();
    }
}
