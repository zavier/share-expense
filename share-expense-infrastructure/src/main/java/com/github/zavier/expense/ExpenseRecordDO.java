package com.github.zavier.expense;

import io.mybatis.provider.Entity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity.Table(value = "expense_record", remark = "费用记录信息", autoResultMap = true)
public class ExpenseRecordDO {
    @Entity.Column(value = "id", remark = "费用记录ID")
    private Integer id;
    @Entity.Column(value = "user_id", remark = "用户ID")
    private Integer userId;
    @Entity.Column(value = "expense_project_id", remark = "费用项目ID")
    private Integer expenseProjectId;
    @Entity.Column(value = "source", remark = "费用来源")
    private String source;
    @Entity.Column(value = "amount", remark = "费用金额")
    private BigDecimal amount;
    @Entity.Column(value = "date", remark = "费用日期")
    private Date date;
    @Entity.Column(value = "is_shared", remark = "是否已均摊")
    private Boolean isShared;
}


