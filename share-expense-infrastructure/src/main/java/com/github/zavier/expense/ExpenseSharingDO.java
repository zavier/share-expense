package com.github.zavier.expense;

import io.mybatis.provider.Entity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity.Table(value = "expense_sharing", remark = "费用均摊记录信息", autoResultMap = true)
public class ExpenseSharingDO {
    @Entity.Column(value = "id", remark = "费用均摊记录ID")
    private Integer id;
    @Entity.Column(value = "record_id", remark = "费用记录ID")
    private Integer recordId;
    @Entity.Column(value = "user_id", remark = "用户ID")
    private Integer userId;
    @Entity.Column(value = "user_name", remark = "用户名称")
    private String userName;
    @Entity.Column(value = "weight", remark = "均摊权重")
    private Integer weight;
    @Entity.Column(value = "amount", remark = "均摊金额")
    private BigDecimal amount;
    @Entity.Column(value = "is_paid", remark = "是否已付款")
    private Boolean isPaid;
    @Entity.Column(value = "created_at", remark = "创建时间")
    private Date createdAt;
    @Entity.Column(value = "updated_at", remark = "更新时间")
    private Date updatedAt;
}





