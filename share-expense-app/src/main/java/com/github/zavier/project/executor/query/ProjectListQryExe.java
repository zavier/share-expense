package com.github.zavier.project.executor.query;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.data.ProjectDTO;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProjectListQryExe {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    public PageResponse<ProjectDTO> execute(ProjectListQry projectListQry) {
        Assert.notNull(projectListQry.getPage(), "页码不能为空");
        Assert.notNull(projectListQry.getSize(), "页大小不能为空");

        final PageResponse<ExpenseProject> projectPageResponse = expenseProjectGateway.pageProject(projectListQry);
        final List<ProjectDTO> projectDTOList = projectPageResponse.getData().stream().map(it -> {
            final ProjectDTO projectDTO = new ProjectDTO();
            projectDTO.setProjectId(it.getId());
            projectDTO.setProjectName(it.getName());
            projectDTO.setProjectDesc(it.getDescription());

            projectDTO.setTotalMember(it.totalMember());
            projectDTO.setTotalExpense(it.totalExpense());
            return projectDTO;
        }).collect(Collectors.toList());

        return PageResponse.of(projectDTOList, projectPageResponse.getTotalCount(), projectPageResponse.getPageSize(), projectPageResponse.getPageIndex());
    }
}
