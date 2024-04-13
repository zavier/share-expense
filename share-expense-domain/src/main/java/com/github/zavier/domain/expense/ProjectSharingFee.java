package com.github.zavier.domain.expense;

import java.util.*;

public class ProjectSharingFee {

    private Map<String, MemberProjectFee> memberFeeMap = new HashMap<>();


    public void addMemberRecordFee(MemberRecordFee memberRecordFee) {
        final MemberProjectFee projectFee = memberFeeMap.getOrDefault(memberRecordFee.getMember(), new MemberProjectFee());
        projectFee.addFeeDetail(memberRecordFee);
        memberFeeMap.put(memberRecordFee.getMember(), projectFee);
    }


    public List<MemberProjectFee> listMemberProjectFee() {
        return Collections.unmodifiableList(new ArrayList<>(memberFeeMap.values()));
    }
}
