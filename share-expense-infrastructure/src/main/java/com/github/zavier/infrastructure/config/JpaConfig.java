package com.github.zavier.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA 配置类
 * <p>
 * 启用 JPA Auditing 功能，自动管理实体的审计字段
 *
 * @author JPA Team
 * @since 2025-01-02
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * 审计用户提供者
     * <p>
     * 返回当前操作用户的 ID，用于 @CreatedBy 和 @LastModifiedBy 审计字段
     * <p>
     * TODO: 集成 Spring Security 后，从 SecurityContext 获取当前登录用户 ID
     * <pre>
     * return () -> {
     *     Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
     *     if (authentication == null || !authentication.isAuthenticated()) {
     *         return Optional.empty();
     *     }
     *     User user = (User) authentication.getPrincipal();
     *     return Optional.of(user.getId());
     * };
     * </pre>
     *
     * @return 当前用户 ID，暂时返回固定值 1
     */
    @Bean
    public AuditorAware<Integer> auditorProvider() {
        return () -> {
            // 暂时返回固定值，待集成 Spring Security 后获取真实用户
            return Optional.of(1);
        };
    }
}
