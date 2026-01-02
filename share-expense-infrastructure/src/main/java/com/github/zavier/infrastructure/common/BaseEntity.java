package com.github.zavier.infrastructure.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA 实体基类
 * <p>
 * 自动管理审计字段：createdAt, updatedAt
 * 使用 JPA Auditing 功能自动填充时间戳
 *
 * @author JPA Team
 * @since 2025-01-02
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * 创建时间
     * 自动填充，不允许更新
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     * 每次实体更新时自动填充
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
