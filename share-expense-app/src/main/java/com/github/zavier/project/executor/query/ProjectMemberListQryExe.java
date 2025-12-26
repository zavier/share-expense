package com.github.zavier.project.executor.query;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectMemberListQry;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class ProjectMemberListQryExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    public List<String> execute(ProjectMemberListQry projectMemberListQry) {
        Assert.notNull(projectMemberListQry.getProjectId(), "项目ID不能为空");
        final Optional<ExpenseProject> projectById = expenseProjectGateway.getProjectById(projectMemberListQry.getProjectId());
        Assert.isTrue(projectById.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectById.get();

        if (!Objects.equals(expenseProject.getCreateUserId(), projectMemberListQry.getOperatorId())) {
            return Collections.emptyList();
        }

        return expenseProject.listAllMember();
    }
}
