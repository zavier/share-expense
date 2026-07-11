package com.github.zavier.project.executor.converter;


import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.MemberProjectFee;
import com.github.zavier.domain.expense.MemberRecordFee;
import com.github.zavier.domain.expense.ProjectSharingFee;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.dto.ExpenseRecordUpdateCmd;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.data.ExpenseProjectMemberDTO;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.dto.data.UserSharingDetailDTO;
import com.github.zavier.project.executor.bo.ExpenseRecordExcelBO;
import com.google.common.base.Joiner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ExpenseProject 聚合装配器
 * <p>
 * 统一管理 ExpenseProject 聚合相关的所有映射：
 * <ul>
 *   <li>Cmd → 领域对象</li>
 *   <li>领域对象 → DTO</li>
 *   <li>领域对象 → Excel BO</li>
 * </ul>
 * 消除了之前分布在 3 个模块、4 个类中的碎片化映射。
 */
public final class ExpenseProjectAssembler {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private ExpenseProjectAssembler() {
        // 工具类，禁止实例化
    }

    // ==================== Cmd → 领域对象 ====================

    /**
     * ProjectAddCmd → ExpenseProject
     */
    public static ExpenseProject toExpenseProject(ProjectAddCmd cmd) {
        final ExpenseProject expenseProject = new ExpenseProject();
        expenseProject.setCreateUserId(cmd.getCreateUserId());
        expenseProject.setName(cmd.getProjectName());
        expenseProject.setDescription(cmd.getProjectDesc());
        expenseProject.setLocked(false);

        expenseProject.addMembers(cmd.getMembers());
        expenseProject.checkUserIdExist();
        expenseProject.checkProjectNameValid();

        return expenseProject;
    }

    /**
     * ExpenseRecordAddCmd → ExpenseRecord
     */
    public static ExpenseRecord toExpenseRecord(ExpenseRecordAddCmd cmd) {
        final ExpenseRecord expenseRecord = new ExpenseRecord();
        expenseRecord.setPayMember(cmd.getPayMember());
        expenseRecord.setProjectId(cmd.getProjectId());
        expenseRecord.setAmount(cmd.getAmount());

        Date payDate = new Date(cmd.getDate() * 1000L);
        expenseRecord.setDate(payDate);
        expenseRecord.setExpenseType(cmd.getExpenseType());
        expenseRecord.setRemark(cmd.getRemark());

        cmd.getConsumerMembers().forEach(expenseRecord::addConsumer);

        return expenseRecord;
    }

    /**
     * ExpenseRecordUpdateCmd → ExpenseRecord
     */
    public static ExpenseRecord toExpenseRecord(ExpenseRecordUpdateCmd cmd) {
        final ExpenseRecord expenseRecord = toExpenseRecord((ExpenseRecordAddCmd) cmd);
        expenseRecord.setId(cmd.getRecordId());
        return expenseRecord;
    }

    // ==================== 领域对象 → DTO ====================

    /**
     * ExpenseRecord → ExpenseRecordDTO
     */
    public static ExpenseRecordDTO toRecordDTO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDTO dto = new ExpenseRecordDTO();
        dto.setRecordId(expenseRecord.getId());
        dto.setPayMember(expenseRecord.getPayMember());
        dto.setExpenseProjectId(expenseRecord.getProjectId());
        dto.setAmount(expenseRecord.getAmount());
        dto.setDate(expenseRecord.getDate().getTime() / 1000);
        dto.setExpenseType(expenseRecord.getExpenseType());
        dto.setRemark(expenseRecord.getRemark());
        dto.setConsumeMembers(new ArrayList<>(expenseRecord.listAllConsumers()));
        return dto;
    }

    /**
     * ExpenseRecord → ExpenseRecordExcelBO
     */
    public static ExpenseRecordExcelBO toExcelBO(ExpenseRecord expenseRecord) {
        final ExpenseRecordExcelBO excelBO = new ExpenseRecordExcelBO();
        excelBO.setDate(new SimpleDateFormat(DATE_FORMAT).format(expenseRecord.getDate()));
        excelBO.setAmount(expenseRecord.getAmount().toPlainString());
        excelBO.setPayMember(expenseRecord.getPayMember());
        excelBO.setExpenseType(expenseRecord.getExpenseType());
        excelBO.setRemark(expenseRecord.getRemark());
        excelBO.setConsumers(Joiner.on(",").join(expenseRecord.listAllConsumers()));
        return excelBO;
    }

    /**
     * ExpenseProject → List&lt;ExpenseProjectMemberDTO&gt;
     */
    public static List<ExpenseProjectMemberDTO> toMemberDTOList(ExpenseProject expenseProject) {
        return expenseProject.listAllMember().stream().map(it -> {
            final ExpenseProjectMemberDTO dto = new ExpenseProjectMemberDTO();
            dto.setMember(it);
            dto.setProjectId(expenseProject.getId());
            return dto;
        }).collect(Collectors.toList());
    }

    // ==================== 结算 → DTO ====================

    /**
     * ProjectSharingFee → List&lt;UserSharingDTO&gt;
     */
    public static List<UserSharingDTO> toSharingDTOList(ProjectSharingFee projectMemberFee) {
        final List<MemberProjectFee> memberProjectFees = projectMemberFee.listMemberProjectFee();

        return memberProjectFees.stream()
                .map(ExpenseProjectAssembler::toSharingDTO)
                .collect(Collectors.toList());
    }

    private static UserSharingDTO toSharingDTO(MemberProjectFee memberProjectFee) {
        final UserSharingDTO sharingDTO = new UserSharingDTO();
        sharingDTO.setMember(memberProjectFee.getMember());
        sharingDTO.setTotalAmount(memberProjectFee.getRecordAmount());
        sharingDTO.setPaidAmount(memberProjectFee.getPaidAmount());
        sharingDTO.setConsumeAmount(memberProjectFee.getConsumeAmount());

        final List<MemberRecordFee> memberFeeDetailList = memberProjectFee.getMemberFeeDetailList();
        final List<UserSharingDetailDTO> children = memberFeeDetailList.stream()
                .map(ExpenseProjectAssembler::toSharingDetailDTO)
                .collect(Collectors.toList());
        sharingDTO.setChildren(children);

        return sharingDTO;
    }

    private static UserSharingDetailDTO toSharingDetailDTO(MemberRecordFee memberRecordFee) {
        final UserSharingDetailDTO detailDTO = new UserSharingDetailDTO();
        detailDTO.setMember(memberRecordFee.getMember());
        detailDTO.setDate(memberRecordFee.getExpenseRecord().getDate().getTime() / 1000);
        detailDTO.setTotalAmount(memberRecordFee.getExpenseRecord().getAmount());
        detailDTO.setPayMember(memberRecordFee.getExpenseRecord().getPayMember());
        detailDTO.setExpenseType(memberRecordFee.getExpenseRecord().getExpenseType());
        detailDTO.setRemark(memberRecordFee.getExpenseRecord().getRemark());
        detailDTO.setPaidAmount(memberRecordFee.getPaidAmount());
        detailDTO.setConsumeAmount(memberRecordFee.getConsumeAmount());
        detailDTO.setConsumeMembers(new ArrayList<>(memberRecordFee.getExpenseRecord().listAllConsumers()));
        return detailDTO;
    }
}
