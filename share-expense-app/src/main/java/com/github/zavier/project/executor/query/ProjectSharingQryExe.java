package com.github.zavier.project.executor.query;

import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.domain.expense.gateway.ExpenseRecordGateway;
import com.github.zavier.dto.ProjectSharingQry;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Component
public class ProjectSharingQryExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;
    @Resource
    private ExpenseRecordGateway expenseRecordGateway;

    public List<ExpenseRecord> execute(ProjectSharingQry projectSharingQry) {
        Assert.notNull(projectSharingQry.getProjectId(), "项目ID不能为空");
        final Optional<ExpenseProject> projectOptional = expenseProjectGateway.getProjectById(projectSharingQry.getProjectId());
        Assert.isTrue(projectOptional.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOptional.get();

        // TODO 迁移到领域对象中 ？

        final List<ExpenseRecord> expenseRecords = expenseRecordGateway.listRecord(expenseProject.getId());

        expenseRecords.forEach(expenseRecord -> {
            expenseProject.listMember().forEach(projectMember -> {
                // TODO
                expenseRecord.addUserSharing(projectMember.getUserId(), projectMember.getUserName(), 0);
            });
        });

        return expenseRecords;
    }

}
