package com.github.zavier.project.executor.converter;

import com.github.zavier.domain.expense.MemberProjectFee;
import com.github.zavier.domain.expense.MemberRecordFee;
import com.github.zavier.domain.expense.ProjectSharingFee;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.dto.data.UserSharingDetailDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SharingConverter {

    public static List<UserSharingDTO> convert(ProjectSharingFee projectMemberFee) {
        final List<MemberProjectFee> memberProjectFees = projectMemberFee.listMemberProjectFee();

        return memberProjectFees.stream()
                .map(SharingConverter::convert)
                .collect(Collectors.toList());
    }

    private static UserSharingDTO convert(MemberProjectFee memberProjectFee) {
        final UserSharingDTO sharingDTO = new UserSharingDTO();
        sharingDTO.setMember(memberProjectFee.getMember());
        sharingDTO.setTotalAmount(memberProjectFee.getRecordAmount());
        sharingDTO.setPaidAmount(memberProjectFee.getPaidAmount());
        sharingDTO.setConsumeAmount(memberProjectFee.getConsumeAmount());

        final List<MemberRecordFee> memberFeeDetailList = memberProjectFee.getMemberFeeDetailList();
        final List<UserSharingDetailDTO> collect = memberFeeDetailList.stream()
                .map(SharingConverter::convert)
                .collect(Collectors.toList());
        sharingDTO.setChildren(collect);

        return sharingDTO;
    }

    private static UserSharingDetailDTO convert(MemberRecordFee memberRecordFee) {
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
