package com.github.zavier.project.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.project.executor.converter.ProjectConverter;
import org.springframework.stereotype.Component;

@Component
public class ProjectAddCmdExe {
    private final ExpenseProjectGateway expenseProjectGateway;

    public ProjectAddCmdExe(ExpenseProjectGateway expenseProjectGateway) {
        this.expenseProjectGateway = expenseProjectGateway;
    }

    public SingleResponse<Integer> execute(ProjectAddCmd projectAddCmd) {
        final ExpenseProject expenseProject = ProjectConverter.convert2AddProject(projectAddCmd);
        expenseProjectGateway.save(expenseProject);
        return SingleResponse.of(expenseProject.getId());
    }
}
