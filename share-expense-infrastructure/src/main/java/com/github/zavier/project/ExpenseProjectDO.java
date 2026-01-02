package com.github.zavier.project;

import com.github.zavier.infrastructure.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 费用项目实体（聚合根）
 * <p>
 * 继承 BaseEntity 获得 JPA Auditing 功能，自动管理 createdAt, updatedAt
 * 使用 @Version 注解实现自动乐观锁
 * <p>
 * DDD 聚合根设计：
 * - 不声明 @OneToMany 关联，通过 Gateway 手动组装聚合
 * - 手动管理子实体生命周期，通过 Repository 直接操作
 * - 子实体保留 projectId 外键字段，支持独立查询和批量操作
 * - 这种设计避免了 JPA 级联操作的性能问题，提供了更精确的控制
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
}
