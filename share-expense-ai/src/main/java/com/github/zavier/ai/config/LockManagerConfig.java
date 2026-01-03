package com.github.zavier.ai.config;

import com.github.zavier.ai.concurrent.LockManager;
import com.github.zavier.ai.concurrent.WeakHashMapLockManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 锁管理器配置类
 *
 * 配置：
 * - 使用 WeakHashMapLockManager
 * - 自动 GC 回收未使用的锁，防止内存泄漏
 * - 无需手动维护
 */
@Configuration
public class LockManagerConfig {

    /**
     * WeakHashMap 锁管理器
     * 自动 GC 回收未使用的锁，防止内存泄漏
     */
    @Bean
    @ConditionalOnMissingBean(LockManager.class)
    public LockManager lockManager() {
        return new WeakHashMapLockManager();
    }
}
