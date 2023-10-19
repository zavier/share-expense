package com.github.zavier.dto.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserSharingDTO {
    private Integer userId;
    private String userName;

    public UserSharingDTO(Integer userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    private BigDecimal shareAmount = BigDecimal.ZERO;

    private BigDecimal paidAmount = BigDecimal.ZERO;


    // 可能为负数
    public BigDecimal getAmount() {
        return shareAmount.subtract(paidAmount);
    }
}