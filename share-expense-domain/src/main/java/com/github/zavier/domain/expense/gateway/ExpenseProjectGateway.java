package com.github.zavier.domain.expense.gateway;

import com.github.zavier.domain.expense.ExpenseProject;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ExpenseProjectGateway {
    void save(ExpenseProject expenseProject);

    Optional<ExpenseProject> getProjectById(@NotNull Integer expenseProjectId);

}
