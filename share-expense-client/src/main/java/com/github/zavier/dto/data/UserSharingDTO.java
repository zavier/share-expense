package com.github.zavier.dto.data;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserSharingDTO {

    private String member;

    /**
     * 参与的记录总金额
     */
    private BigDecimal totalAmount;

    /**
     * 本人支出金额
     */
    private BigDecimal paidAmount;
    /**
     * 本人消费金额
     */
    private BigDecimal consumeAmount;

    /**
     * 明细
     */
    private List<UserSharingDetailDTO> children = new ArrayList<>();

}