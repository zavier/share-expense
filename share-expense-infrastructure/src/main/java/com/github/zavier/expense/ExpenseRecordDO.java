package com.github.zavier.expense;

import com.github.zavier.infrastructure.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 费用记录实体
 * <p>
 * DDD 设计说明：
 * - 继承 BaseEntity 获得 JPA Auditing 功能
 * - 使用 @Version 注解实现自动乐观锁
 * - 使用单向 @OneToMany 关联消费人员
 * - 不设置 CascadeType，手动管理子实体生命周期
 * - 使用 FetchType.LAZY 避免性能问题
 * - 保留子实体的 recordId 字段以支持查询
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

    /**
     * 消费人员列表（一对多单向关联）
     * <p>
     * 设计说明：
     * - 单向关联：不添加 mappedBy，子实体不添加反向 @ManyToOne
     * - LAZY 加载：避免查询记录时自动加载所有消费人员
     * - 不设置 CascadeType：手动管理消费人员的增删改
     * - @JoinColumn：指定外键字段为 record_id
     * <p>
     * 注意：虽然 JPA 会自动维护关联关系，但为了查询方便，
     * ExpenseRecordConsumerDO 仍保留 recordId 字段
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private List<ExpenseRecordConsumerDO> consumers = new ArrayList<>();
}