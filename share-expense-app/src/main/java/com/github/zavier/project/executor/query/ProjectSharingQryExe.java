package com.github.zavier.project.executor.query;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.dto.ProjectSharingQry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProjectSharingQryExe {

    private final ExpenseProjectGateway expenseProjectGateway;
    private final ExpenseRecordGateway expenseRecordGateway;

    public ProjectSharingQryExe(ExpenseProjectGateway expenseProjectGateway, ExpenseRecordGateway expenseRecordGateway) {
        this.expenseProjectGateway = expenseProjectGateway;
        this.expenseRecordGateway = expenseRecordGateway;
    }


    public List<ExpenseRecord> execute(ProjectSharingQry projectSharingQry) {
        Assert.notNull(projectSharingQry.getProjectId(), "项目ID不能为空");
        final Optional<ExpenseProject> projectOptional = expenseProjectGateway.getProjectById(projectSharingQry.getProjectId());
        Assert.isTrue(projectOptional.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOptional.get();

        // TODO 迁移到领域对象中 ？

        final List<ExpenseRecord> expenseRecords = expenseRecordGateway.listRecord(expenseProject.getId());

        expenseRecords.forEach(expenseRecord -> {
            expenseProject.listMember().forEach(projectMember -> {
                expenseRecord.addUserSharing(projectMember.getUserId(), projectMember.getUserName(), projectMember.getWeight());
            });
        });

        return expenseRecords;
    }

}
