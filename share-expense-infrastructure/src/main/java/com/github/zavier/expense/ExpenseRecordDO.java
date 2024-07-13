package com.github.zavier.expense;

import io.mybatis.provider.Entity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity.Table(value = "expense_record", remark = "费用记录信息", autoResultMap = true)
public class ExpenseRecordDO {
    @Entity.Column(value = "id", remark = "费用记录ID", id = true)
    private Integer id;
    @Entity.Column(value = "pay_member", remark = "付款的用户")
    private String payMember;
    @Entity.Column(value = "project_id", remark = "费用项目ID")
    private Integer projectId;
    @Entity.Column(value = "amount", remark = "费用金额")
    private BigDecimal amount;
    @Entity.Column(value = "pay_date", remark = "费用日期")
    private Date payDate;
    @Entity.Column(value = "expense_type", remark = "费用类型")
    private String expenseType;
    @Entity.Column(value = "remark", remark = "备注")
    private String remark;
    @Entity.Column(value = "created_at", remark = "创建时间")
    private Date createdAt;
    @Entity.Column(value = "updated_at", remark = "更新时间")
    private Date updatedAt;
}


