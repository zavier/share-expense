package com.github.zavier.dto.data;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserSharingDetailDTO {

    private String member;

    // 秒 时间戳
    private Long date;

    /**
     * 总金额
     */
    private BigDecimal amount;

    /**
     * 支付人
     */
    private String payMember;

    /**
     * 费用类型
     */
    private String expenseType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 本人支出金额
     */
    private BigDecimal paidAmount;
    /**
     * 本人消费金额
     */
    private BigDecimal consumeAmount;

    /**
     * 参与消费人员
     */
    private List<String> consumeMembers = new ArrayList<>();
}