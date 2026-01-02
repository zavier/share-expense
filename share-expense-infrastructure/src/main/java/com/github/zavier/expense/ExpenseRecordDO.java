package com.github.zavier.expense;

import com.github.zavier.infrastructure.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用记录实体
 * <p>
 * DDD 设计说明：
 * - 继承 BaseEntity 获得 JPA Auditing 功能
 * - 使用 @Version 注解实现自动乐观锁
 * - 不声明 @OneToMany 关联消费人员，通过 Gateway 手动组装
 * - 手动管理消费人员生命周期，通过 Repository 直接操作
 * - 消费人员保留 recordId 外键字段，支持独立查询和批量操作
 * - 使用 LocalDateTime 替代 Date（Java 8+ 时间 API）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "expense_record")
public class ExpenseRecordDO extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "pay_member")
    private String payMember;

    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "pay_date")
    private LocalDateTime payDate;

    @Column(name = "expense_type")
    private String expenseType;

    @Column(name = "remark")
    private String remark;

    /**
     * 乐观锁版本号
     * 使用 @Version 注解，JPA 自动管理并发控制
     */
    @Version
    @Column(name = "version")
    private Integer version;
}