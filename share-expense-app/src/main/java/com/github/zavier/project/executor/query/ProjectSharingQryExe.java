package com.github.zavier.project.executor.query;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ProjectSharingFee;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectSharingQry;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;

@Component
public class ProjectSharingQryExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    public ProjectSharingFee execute(ProjectSharingQry projectSharingQry) {
        Assert.notNull(projectSharingQry.getProjectId(), "项目ID不能为空");
        final Optional<ExpenseProject> projectOptional = expenseProjectGateway.getProjectById(projectSharingQry.getProjectId());
        Assert.isTrue(projectOptional.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOptional.get();

        Assert.isTrue(Objects.equals(expenseProject.getCreateUserId(), projectSharingQry.getOperatorId()), "没有权限查看");

        return expenseProject.calcMemberSharingFee();
    }

}
