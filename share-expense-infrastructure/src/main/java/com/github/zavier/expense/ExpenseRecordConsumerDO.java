package com.github.zavier.expense;

import com.github.zavier.infrastructure.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 费用记录消费人员实体
 * <p>
 * 继承 BaseEntity 获得 JPA Auditing 功能，自动管理 createdAt, updatedAt
 * <p>
 * 注意：此实体不添加 @Version，因为它是聚合内部的最小子实体
 * 通过父实体（ExpenseRecordDO）的版本控制即可保证并发安全
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "expense_record_consumer")
public class ExpenseRecordConsumerDO extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "record_id")
    private Integer recordId;

    @Column(name = "member")
    private String member;
}
