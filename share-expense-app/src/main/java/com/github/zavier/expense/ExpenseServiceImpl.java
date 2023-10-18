package com.github.zavier.expense;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ExpenseService;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.ExpenseSharing;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.dto.ExpenseRecordQry;
import com.github.zavier.dto.ExpenseRecordSharingAddCmd;
import com.github.zavier.dto.ExpenseRecordSharingListQry;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.ExpenseRecordSharingDTO;
import com.github.zavier.expense.executor.ExpenseRecordAddCmdExe;
import com.github.zavier.expense.executor.ExpenseRecordListQryExe;
import com.github.zavier.expense.executor.ExpenseRecordSharingAddCmdExe;
import com.github.zavier.expense.executor.ExpenseRecordSharingListQryExe;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// TODO 合并到 projectService ?
@Service
@CatchAndLog
public class ExpenseServiceImpl implements ExpenseService {

    @Resource
    private ExpenseRecordAddCmdExe expenseRecordAddCmdExe;
    @Resource
    private ExpenseRecordSharingAddCmdExe expenseRecordSharingAddCmdExe;
    @Resource
    private ExpenseRecordListQryExe expenseRecordListQryExe;
    @Resource
    private ExpenseRecordSharingListQryExe expenseRecordSharingListQryExe;

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
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return SingleResponse.of(collect);

    }

    @Override
    public Response addExpenseRecordSharing(ExpenseRecordSharingAddCmd sharingAddCmd) {
        expenseRecordSharingAddCmdExe.execute(sharingAddCmd);
        return Response.buildSuccess();
    }

    @Override
    public SingleResponse<List<ExpenseRecordSharingDTO>> listRecordSharing(ExpenseRecordSharingListQry expenseRecordSharingQry){
        final List<ExpenseSharing> execute = expenseRecordSharingListQryExe.execute(expenseRecordSharingQry);
        final List<ExpenseRecordSharingDTO> collect = execute.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return SingleResponse.of(collect);
    }

    private ExpenseRecordDTO convertToDTO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDTO expenseRecordDTO = new ExpenseRecordDTO();
        expenseRecordDTO.setRecordId(expenseRecord.getId());
        expenseRecordDTO.setUserId(expenseRecord.getCostUserId());
        expenseRecordDTO.setUserName(expenseRecord.getCostUserName());
        expenseRecordDTO.setExpenseProjectId(expenseRecord.getProjectId());
        expenseRecordDTO.setAmount(expenseRecord.getAmount());
        expenseRecordDTO.setDate(expenseRecord.getDate());
        expenseRecordDTO.setExpenseType(expenseRecord.getExpenseType());
        expenseRecordDTO.setRemark(expenseRecord.getRemark());
        return expenseRecordDTO;
    }

    private ExpenseRecordSharingDTO convertToDTO(ExpenseSharing expenseSharing) {
        final ExpenseRecordSharingDTO sharingDTO = new ExpenseRecordSharingDTO();
        sharingDTO.setUserId(expenseSharing.getUserId());
        sharingDTO.setUserName(expenseSharing.getUserName());
        sharingDTO.setWeight(expenseSharing.getWeight());
        sharingDTO.setAmount(expenseSharing.getAmount());
        return sharingDTO;
    }
}
