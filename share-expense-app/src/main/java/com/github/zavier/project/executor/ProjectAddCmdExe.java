package com.github.zavier.project.executor;

import com.alibaba.cola.dto.Response;
import com.github.zavier.domain.common.ChangingStatus;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectAddCmd;
import org.springframework.stereotype.Component;

@Component
public class ProjectAddCmdExe {
    private final ExpenseProjectGateway expenseProjectGateway;

    public ProjectAddCmdExe(ExpenseProjectGateway expenseProjectGateway) {
        this.expenseProjectGateway = expenseProjectGateway;
    }

    public Response execute(ProjectAddCmd projectAddCmd) {
        final ExpenseProject expenseProject = new ExpenseProject();
        expenseProject.setUserId(projectAddCmd.getUserId());
        expenseProject.setName(projectAddCmd.getProjectName());
        expenseProject.setDescription(projectAddCmd.getProjectDesc());

        expenseProject.checkUserIdExist();
        expenseProject.checkProjectNameValid();

        expenseProject.setChangingStatus(ChangingStatus.NEW);
        expenseProjectGateway.save(expenseProject);
        return Response.buildSuccess();
    }
}
