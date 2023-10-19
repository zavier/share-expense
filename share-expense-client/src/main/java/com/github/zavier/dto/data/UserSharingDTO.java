package com.github.zavier.dto.data;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class UserSharingDTO {
    private Integer userId;
    private String userName;

    // 可能为负数
    private BigDecimal amount;

}