package com.github.zavier.project;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.ProjectSharingFee;
import com.github.zavier.domain.expense.domainservice.ExpenseRecordValidator;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.*;
import com.github.zavier.dto.data.*;
import com.github.zavier.dto.data.statistics.PieStatisticsDTO;
import com.github.zavier.project.executor.bo.ExpenseRecordExcelBO;
import com.github.zavier.project.executor.converter.ExpenseProjectAssembler;
import com.github.zavier.utils.FreemarkerUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@CatchAndLog
public class ExpenseApplicationService {

    @Resource
    private ExpenseProjectGateway expenseProjectGateway;

    @Resource
    private ExpenseRecordValidator expenseRecordValidator;

    // ==================== 项目操作 ====================

    public SingleResponse<Integer> createProject(ProjectAddCmd projectAddCmd) {
        final ExpenseProject expenseProject = ExpenseProjectAssembler.toExpenseProject(projectAddCmd);
        expenseProjectGateway.save(expenseProject);
        return SingleResponse.of(expenseProject.getId());
    }

    public Response deleteProject(Integer projectId, Integer operatorId) {
        Assert.notNull(projectId, "项目ID不能为空");
        Assert.notNull(operatorId, "操作人ID不能为空");

        final ExpenseProject expenseProject = getAuthorizedProject(projectId, operatorId);
        expenseProjectGateway.delete(projectId);
        return Response.buildSuccess();
    }

    public Response addProjectMember(ProjectMemberAddCmd cmd) {
        Assert.notNull(cmd.getProjectId(), "项目ID不能为空");
        Assert.notEmpty(cmd.getMembers(), "成员信息不能为空");
        cmd.getMembers().forEach(it -> {
            Assert.notNull(it, "成员信息不能为空");
            Assert.isTrue(StringUtils.isNotBlank(it), "成员信息不能为空");
        });

        final ExpenseProject expenseProject = getAuthorizedProject(cmd.getProjectId(), cmd.getOperatorId());
        cmd.getMembers().forEach(expenseProject::addMember);
        expenseProjectGateway.save(expenseProject);
        return Response.buildSuccess();
    }

    public SingleResponse<List<ExpenseProjectMemberDTO>> listProjectMember(ProjectMemberListQry qry) {
        Assert.notNull(qry.getProjectId(), "项目ID不能为空");
        final Optional<ExpenseProject> projectById = expenseProjectGateway.getProjectById(qry.getProjectId());
        Assert.isTrue(projectById.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectById.get();

        if (!expenseProject.isOwnedBy(qry.getOperatorId())) {
            return SingleResponse.of(Collections.emptyList());
        }

        final List<ExpenseProjectMemberDTO> collect = ExpenseProjectAssembler.toMemberDTOList(expenseProject);
        return SingleResponse.of(collect);
    }

    public PageResponse<ProjectDTO> pageProject(ProjectListQry projectListQry) {
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

    // ==================== 费用记录操作 ====================

    public Response addExpenseRecord(ExpenseRecordAddCmd cmd) {
        log.info("expenseRecordAddCmd: {}", cmd);
        expenseRecordValidator.valid(cmd);

        final ExpenseProject expenseProject = getAuthorizedProject(cmd.getProjectId(), cmd.getOperatorId());

        final ExpenseRecord expenseRecord = ExpenseProjectAssembler.toExpenseRecord(cmd);
        expenseProject.addExpenseRecord(expenseRecord);
        expenseProjectGateway.save(expenseProject);
        return Response.buildSuccess();
    }

    public Response updateExpenseRecord(ExpenseRecordUpdateCmd cmd) {
        log.info("expenseRecordUpdateCmd: {}", cmd);
        expenseRecordValidator.valid(cmd);

        final ExpenseProject expenseProject = getAuthorizedProject(cmd.getProjectId(), cmd.getOperatorId());

        final ExpenseRecord expenseRecord = ExpenseProjectAssembler.toExpenseRecord(cmd);
        expenseProject.updateExpenseRecord(expenseRecord);
        expenseProjectGateway.save(expenseProject);
        return Response.buildSuccess();
    }

    public Response deleteExpenseRecord(ExpenseRecordDeleteCmd cmd) {
        log.info("ExpenseRecordDeleteCmd:{}", cmd);
        Assert.notNull(cmd.getProjectId(), "项目id不能为空");
        Assert.notNull(cmd.getRecordId(), "记录id不能为空");
        Assert.notNull(cmd.getOperatorId(), "操作人不能为空");

        final ExpenseProject expenseProject = getAuthorizedProject(cmd.getProjectId(), cmd.getOperatorId());

        expenseProject.removeRecord(cmd.getRecordId());
        expenseProjectGateway.save(expenseProject);
        return Response.buildSuccess();
    }

    public SingleResponse<List<ExpenseRecordDTO>> listRecord(ExpenseRecordQry qry) {
        final Integer projectId = qry.getProjectId();

        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectId);
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");

        if (!projectOpt.get().isOwnedBy(qry.getOperatorId())) {
            return SingleResponse.of(Collections.emptyList());
        }

        final List<ExpenseRecordDTO> collect = projectOpt.get().listAllExpenseRecord().stream()
                .map(ExpenseProjectAssembler::toRecordDTO)
                .collect(Collectors.toList());
        return SingleResponse.of(collect);
    }

    // ==================== 结算与导出 ====================

    public SingleResponse<List<UserSharingDTO>> getProjectSharingDetail(ProjectSharingQry qry) {
        Assert.notNull(qry.getProjectId(), "项目ID不能为空");
        final Optional<ExpenseProject> projectOptional = expenseProjectGateway.getProjectById(qry.getProjectId());
        Assert.isTrue(projectOptional.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOptional.get();

        Assert.isTrue(Objects.equals(expenseProject.getCreateUserId(), qry.getOperatorId()), "没有权限查看");

        final ProjectSharingFee projectSharingFee = expenseProject.calcMemberSharingFee();
        final List<UserSharingDTO> sharingDTOList = ExpenseProjectAssembler.toSharingDTOList(projectSharingFee);
        return SingleResponse.of(sharingDTOList);
    }

    public SingleResponse<List<ExpenseRecordExcelBO>> exportRecords(Integer projectId, Integer operatorId) {
        final ExpenseProject expenseProject = getAuthorizedProject(projectId, operatorId);

        final List<ExpenseRecord> expenseRecords = expenseProject.listAllExpenseRecord();
        final List<ExpenseRecordExcelBO> collect = expenseRecords.stream()
                .map(ExpenseProjectAssembler::toExcelBO)
                .map(it -> {
                    it.setProjectName(expenseProject.getName());
                    return it;
                })
                .collect(Collectors.toList());
        return SingleResponse.of(collect);
    }

    // ==================== 统计 ====================

    public SingleResponse<String> statisticsByExpenseType(Integer projectId, Integer operatorId) {
        final ExpenseProject project = getAuthorizedProject(projectId, operatorId);

        final List<ExpenseRecord> expenseRecords = project.listAllExpenseRecord();
        final String result = parsePieEChartConfig(expenseRecords);
        return SingleResponse.of(result);
    }

    // ==================== 私有方法 ====================

    /**
     * 加载聚合并校验权限。权限校验失败时抛出异常。
     */
    private ExpenseProject getAuthorizedProject(Integer projectId, Integer operatorId) {
        final Optional<ExpenseProject> projectOpt = expenseProjectGateway.getProjectById(projectId);
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
        final ExpenseProject expenseProject = projectOpt.get();
        Assert.isTrue(expenseProject.isOwnedBy(operatorId), "无权限");
        return expenseProject;
    }

    private static String parsePieEChartConfig(List<ExpenseRecord> expenseRecords) {
        final Map<String, LongSummaryStatistics> map = expenseRecords.stream()
                .collect(Collectors.groupingBy(ExpenseRecord::getExpenseType,
                        Collectors.summarizingLong(it -> it.getAmount().multiply(BigDecimal.valueOf(100)).longValue())));
        final Long total = map.values().stream().map(LongSummaryStatistics::getSum).reduce(0L, Long::sum);

        List<PieStatisticsDTO> list = new ArrayList<>();
        map.forEach((type, value) -> {
            final PieStatisticsDTO dto = new PieStatisticsDTO();
            final String percent = BigDecimal.valueOf(value.getSum())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_DOWN)
                    .toPlainString();
            dto.setLabel(type + "(" + percent + "%)");
            dto.setValue(value.getSum());
            list.add(dto);
        });

        String templateName = "pieStatistics.ftl";
        Map<String, Object> data = new HashMap<>();
        data.put("title", "费用类型信息");
        data.put("dataList", list);

        return FreemarkerUtils.processTemplate(templateName, data);
    }
}
