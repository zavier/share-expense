package com.github.zavier.mock;

import com.alibaba.cola.dto.PageResponse;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectListQry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ExpenseProjectGatewayMock implements ExpenseProjectGateway {

    private Map<Integer, ExpenseProject> projectMap = new HashMap<>();

    private AtomicInteger id = new AtomicInteger(1);

    @Override
    public void save(ExpenseProject expenseProject) {
        if (expenseProject.getId() != null) {
            projectMap.put(expenseProject.getId(), expenseProject);
            return;
        }
        projectMap.put(id.incrementAndGet(), expenseProject);
        expenseProject.setId(id.get());
    }

    @Override
    public void delete(Integer projectId) {
        projectMap.remove(projectId);
    }

    @Override
    public Optional<ExpenseProject> getProjectById(@NotNull Integer expenseProjectId) {
        return Optional.ofNullable(projectMap.get(expenseProjectId));
    }

    @Override
    public PageResponse<ExpenseProject> pageProject(ProjectListQry projectListQry) {
        return null;
    }
}
