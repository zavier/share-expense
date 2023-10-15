package com.github.zavier.domain.expense.gateway;

import com.github.zavier.domain.expense.ExpenseRecord;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface ExpenseRecordGateway {
    void save(ExpenseRecord expenseRecord);

    Optional<ExpenseRecord> getRecordById(@NotNull Integer recordId);

    List<ExpenseRecord> listRecord(@NotNull Integer projectId);
}
