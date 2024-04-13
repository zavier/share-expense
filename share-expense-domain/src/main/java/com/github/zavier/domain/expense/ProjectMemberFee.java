package com.github.zavier.domain.expense;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectMemberFee {

    private final List<MemberFee> memberFeeList = new ArrayList<>();

    public void addMemberFee(MemberFee memberFee) {
        memberFeeList.add(memberFee);
    }

    public List<MemberFee> mergeFeeByMember() {
        List<MemberFee> result = new ArrayList<>();

        final Map<String, List<MemberFee>> memberFeeMap = memberFeeList.stream()
                .collect(Collectors.groupingBy(MemberFee::getMember));
        memberFeeMap.forEach((member, feeList) -> {

            final MemberFee memberFee = new MemberFee();
            memberFee.setMember(member);
            memberFee.setRecordAmount(feeList.stream()
                    .map(MemberFee::getRecordAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            memberFee.setConsumeAmount(feeList.stream()
                    .map(MemberFee::getConsumeAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            memberFee.setPaidAmount(feeList.stream()
                    .map(MemberFee::getPaidAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(memberFee);
        });

        return result;
    }

}
