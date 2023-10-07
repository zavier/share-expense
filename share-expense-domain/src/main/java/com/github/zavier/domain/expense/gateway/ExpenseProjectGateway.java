package com.github.zavier.domain.expense.gateway;

import com.alibaba.cola.dto.PageResponse;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.dto.ProjectListQry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ExpenseProjectGateway {
    void save(ExpenseProject expenseProject);

    void delete(Integer projectId);

    Optional<ExpenseProject> getProjectById(@NotNull Integer expenseProjectId);

    PageResponse<ExpenseProject> pageProject(ProjectListQry projectListQry);
}
