package com.github.zavier.project.executor.converter;

import com.github.zavier.domain.expense.MemberFee;
import com.github.zavier.domain.expense.ProjectMemberFee;
import com.github.zavier.dto.data.UserSharingDTO;

import java.util.List;
import java.util.stream.Collectors;

public class SharingConverter {

    public static List<UserSharingDTO> convert(ProjectMemberFee projectMemberFee) {
        final List<MemberFee> memberFees = projectMemberFee.mergeFeeByMember();
        return memberFees.stream()
                .map(fee -> {
                    final UserSharingDTO sharingDTO = new UserSharingDTO();
                    sharingDTO.setMember(fee.getMember());
                    sharingDTO.setTotalAmount(fee.getRecordAmount());
                    sharingDTO.setPaidAmount(fee.getPaidAmount());
                    sharingDTO.setConsumeAmount(fee.getConsumeAmount());
                    return sharingDTO;
                }).collect(Collectors.toList());
    }
}
