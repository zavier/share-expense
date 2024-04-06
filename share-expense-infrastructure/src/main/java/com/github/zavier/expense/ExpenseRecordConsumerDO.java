package com.github.zavier.expense;

import io.mybatis.provider.Entity;
import lombok.Data;

import java.util.Date;

@Data
@Entity.Table(value = "expense_record_consumer", remark = "费用记录消费人信息", autoResultMap = true)
public class ExpenseRecordConsumerDO {
    @Entity.Column(value = "id", remark = "费用记录ID", id = true)
    private Integer id;
    @Entity.Column(value = "project_id", remark = "费用项目ID")
    private Integer projectId;
    @Entity.Column(value = "record_id", remark = "费用记录ID")
    private Integer recordId;
    @Entity.Column(value = "consumer_id", remark = "消费用户ID")
    private Integer consumerId;
    @Entity.Column(value = "consumer_name", remark = "消费用户名称")
    private String consumerName;
    @Entity.Column(value = "created_at", remark = "创建时间")
    private Date createdAt;
    @Entity.Column(value = "updated_at", remark = "更新时间")
    private Date updatedAt;
}


