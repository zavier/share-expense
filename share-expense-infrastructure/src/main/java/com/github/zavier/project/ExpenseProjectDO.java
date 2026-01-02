package com.github.zavier.project;

import com.github.zavier.expense.ExpenseRecordDO;
import com.github.zavier.infrastructure.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 费用项目实体（聚合根）
 * <p>
 * 继承 BaseEntity 获得 JPA Auditing 功能，自动管理 createdAt, updatedAt
 * 使用 @Version 注解实现自动乐观锁
 * <p>
 * DDD 聚合根设计：
 * - 使用单向 @OneToMany 关联子实体
 * - 不设置 CascadeType，手动管理子实体生命周期
 * - 使用 FetchType.LAZY 避免性能问题
 * - 保留子实体的 projectId 字段以支持查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "expense_project")
public class ExpenseProjectDO extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "create_user_id")
    private Integer createUserId;

    @Column(name = "locked")
    private Boolean locked;

    @Column(name = "description")
    private String description;

    /**
     * 乐观锁版本号
     * 使用 @Version 注解，JPA 自动管理并发控制
     * 每次更新时自动递增，检测并发冲突
     */
    @Version
    @Column(name = "version")
    private Integer version;

    /**
     * 项目成员列表（一对多单向关联）
     * <p>
     * 设计说明：
     * - 单向关联：不添加 mappedBy，子实体不添加反向 @ManyToOne
     * - LAZY 加载：避免查询项目时自动加载所有成员
     * - 不设置 CascadeType：手动管理成员的增删改
     * - @JoinColumn：指定外键字段为 project_id
     * <p>
     * 注意：虽然 JPA 会自动维护关联关系，但为了查询方便，
     * ExpenseProjectMemberDO 仍保留 projectId 字段
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private List<ExpenseProjectMemberDO> members = new ArrayList<>();

    /**
     * 费用记录列表（一对多单向关联）
     * <p>
     * 设计说明：
     * - 单向关联：不添加 mappedBy，子实体不添加反向 @ManyToOne
     * - LAZY 加载：避免查询项目时自动加载所有记录
     * - 不设置 CascadeType：手动管理记录的增删改
     * - @JoinColumn：指定外键字段为 project_id
     * <p>
     * 注意：虽然 JPA 会自动维护关联关系，但为了查询方便，
     * ExpenseRecordDO 仍保留 projectId 字段
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private List<ExpenseRecordDO> expenseRecords = new ArrayList<>();
}
