package com.github.zavier.expense;

import io.mybatis.provider.Entity;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity.Table(value = "expense_sharing", remark = "费用均摊记录信息", autoResultMap = true)
public class ExpenseSharingDO {
    @Entity.Column(value = "id", remark = "费用均摊记录ID")
    private Integer id;
    @Entity.Column(value = "expense_record_id", remark = "费用记录ID")
    private Integer expenseRecordId;
    @Entity.Column(value = "user_id", remark = "用户ID")
    private Integer userId;
    @Entity.Column(value = "weight", remark = "均摊权重")
    private Integer weight;
    @Entity.Column(value = "amount", remark = "均摊金额")
    private BigDecimal amount;
}





