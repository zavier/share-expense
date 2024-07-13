package com.github.zavier.project.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.project.executor.bo.ExpenseRecordExcelBO;
import com.github.zavier.project.executor.converter.ExpenseRecordConverter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ExpenseRecordExportExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    public SingleResponse<List<ExpenseRecordExcelBO>> execute(Integer projectId, Integer operatorId) {
        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectId);
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOpt.get();

        Assert.isTrue(Objects.equals(expenseProject.getCreateUserId(), operatorId), "无权限");

        final List<ExpenseRecord> expenseRecords = expenseProject.listAllExpenseRecord();
        final List<ExpenseRecordExcelBO> collect = expenseRecords.stream()
                .map(ExpenseRecordConverter::convert)
                .map(it -> {
                    // 设置一下项目名称
                    it.setProjectName(expenseProject.getName());
                    return it;
                })
                .collect(Collectors.toList());
        return SingleResponse.of(collect);
    }
}
