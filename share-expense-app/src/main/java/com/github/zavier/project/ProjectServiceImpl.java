package com.github.zavier.project;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.domain.expense.ExpenseProjectMember;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.ExpenseSharing;
import com.github.zavier.dto.*;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.project.executor.*;
import com.github.zavier.project.executor.converter.ProjectConverter;
import com.github.zavier.project.executor.query.ExpenseRecordListQryExe;
import com.github.zavier.project.executor.query.ProjectListQryExe;
import com.github.zavier.project.executor.query.ProjectMemberListQryExe;
import com.github.zavier.project.executor.query.ProjectSharingQryExe;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@CatchAndLog
public class ProjectServiceImpl implements ProjectService {

    private final ProjectAddCmdExe projectAddCmdExe;
    private final ProjectMemberAddCmdExe projectMemberAddCmdExe;
    private final ProjectDeleteCmdExe projectDeleteCmdExe;
    private final ProjectListQryExe projectListQryExe;
    private final ProjectMemberListQryExe projectMemberListQryExe;
    private final ExpenseRecordAddCmdExe expenseRecordAddCmdExe;
    private final ExpenseRecordSharingAddCmdExe expenseRecordSharingAddCmdExe;
    private final ExpenseRecordListQryExe expenseRecordListQryExe;
    private final ProjectSharingQryExe projectSharingQryExe;

    public ProjectServiceImpl(ProjectAddCmdExe projectAddCmdExe,
                              ProjectMemberAddCmdExe projectMemberAddCmdExe,
                              ProjectDeleteCmdExe projectDeleteCmdExe,
                              ProjectListQryExe projectListQryExe,
                              ProjectMemberListQryExe projectMemberListQryExe,
                              ExpenseRecordAddCmdExe expenseRecordAddCmdExe,
                              ExpenseRecordSharingAddCmdExe expenseRecordSharingAddCmdExe,
                              ExpenseRecordListQryExe expenseRecordListQryExe,
                              ProjectSharingQryExe projectSharingQryExe) {
        this.projectAddCmdExe = projectAddCmdExe;
        this.projectMemberAddCmdExe = projectMemberAddCmdExe;
        this.projectDeleteCmdExe = projectDeleteCmdExe;
        this.projectListQryExe = projectListQryExe;
        this.projectMemberListQryExe = projectMemberListQryExe;
        this.expenseRecordAddCmdExe = expenseRecordAddCmdExe;
        this.expenseRecordSharingAddCmdExe = expenseRecordSharingAddCmdExe;
        this.expenseRecordListQryExe = expenseRecordListQryExe;
        this.projectSharingQryExe = projectSharingQryExe;
    }

    @Override
    public Response createProject(ProjectAddCmd projectAddCmd) {
        return projectAddCmdExe.execute(projectAddCmd);
    }

    @Override
    public Response addProjectMember(ProjectMemberAddCmd projectMemberAddCmd) {
        return projectMemberAddCmdExe.addProjectMember(projectMemberAddCmd);
    }

    @Override
    public SingleResponse<List<ExpenseProjectMemberDTO>> listProjectMember(ProjectMemberListQry projectMemberListQry) {
        final List<ExpenseProjectMember> execute = projectMemberListQryExe.execute(projectMemberListQry);
        final List<ExpenseProjectMemberDTO> collect = execute.stream()
                .map(ProjectConverter::convertToDTO)
                .collect(Collectors.toList());

        return SingleResponse.of(collect);
    }

    @Override
    public Response deleteProject(ProjectDeleteCmd projectDeleteCmd) {
        return projectDeleteCmdExe.execute(projectDeleteCmd.getProjectId(), projectDeleteCmd.getOperatorId());
    }

    @Override
    public PageResponse<ProjectDTO> pageProject(ProjectListQry projectListQry) {
        return projectListQryExe.execute(projectListQry);
    }

    @Override
    public Response addExpenseRecord(ExpenseRecordAddCmd expenseRecordAddCmd) {
        expenseRecordAddCmdExe.execute(expenseRecordAddCmd);
        return Response.buildSuccess();
    }

    @Override
    public SingleResponse<List<ExpenseRecordDTO>> listRecord(ExpenseRecordQry expenseRecordQry) {
        final SingleResponse<List<ExpenseRecord>> response = expenseRecordListQryExe.execute(expenseRecordQry);
        if (!response.isSuccess()) {
            return SingleResponse.buildFailure(response.getErrCode(), response.getErrMessage());
        }
        final List<ExpenseRecord> data = response.getData();
        if (CollectionUtils.isEmpty(data)) {
            return SingleResponse.of(Collections.emptyList());
        }

        final List<ExpenseRecordDTO> collect = data.stream()
                .map(ProjectConverter::convertToDTO)
                .collect(Collectors.toList());

        return SingleResponse.of(collect);

    }

    @Override
    public Response addExpenseRecordSharing(ExpenseRecordSharingAddCmd sharingAddCmd) {
        expenseRecordSharingAddCmdExe.execute(sharingAddCmd);
        return Response.buildSuccess();
    }


    @Override
    public SingleResponse<List<UserSharingDTO>> getProjectSharingDetail(ProjectSharingQry projectSharingQry) {
        Map<Integer, UserSharingDTO> sharingDTOMap = new HashMap<>();

        // TODO 迁移到领域对象中 ?
        final List<ExpenseRecord> execute = projectSharingQryExe.execute(projectSharingQry);
        execute.forEach(expenseRecord -> {
            // 需要均摊的
            final Map<Integer, ExpenseSharing> userIdSharingMap = expenseRecord.getUserIdSharingMap();
            userIdSharingMap.forEach((userId, sharing) -> {
                final UserSharingDTO orDefault = sharingDTOMap.getOrDefault(userId, new UserSharingDTO(userId, sharing.getUserName()));
                orDefault.setShareAmount(orDefault.getShareAmount().add(sharing.getAmount()));
                sharingDTOMap.put(userId, orDefault);
            });

            // 减去自己花费的
            final Integer costUserId = expenseRecord.getCostUserId();
            final BigDecimal amount = expenseRecord.getAmount();
            final UserSharingDTO orDefault = sharingDTOMap.getOrDefault(costUserId, new UserSharingDTO(costUserId, expenseRecord.getCostUserName()));
            orDefault.setPaidAmount(orDefault.getPaidAmount().add(amount));
            sharingDTOMap.put(costUserId, orDefault);
        });

        return SingleResponse.of(new ArrayList<>(sharingDTOMap.values()));
    }
}
