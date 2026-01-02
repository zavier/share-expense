package com.github.zavier.project;

import com.github.zavier.infrastructure.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目成员实体
 * <p>
 * 继承 BaseEntity 获得 JPA Auditing 功能，自动管理 createdAt, updatedAt
 * 使用 @Version 注解实现自动乐观锁
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "expense_project_member")
public class ExpenseProjectMemberDO extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "name")
    private String name;

    /**
     * 乐观锁版本号
     * 使用 @Version 注解，JPA 自动管理并发控制
     */
    @Version
    @Column(name = "version")
    private Integer version;
}