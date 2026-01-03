package com.github.zavier.ai.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 锁管理器接口
 * 提供统一的锁管理能力，支持本地锁和分布式锁
 *
 * 使用示例：
 * <pre>
 * try (LockContext context = lockManager.acquireLock(key, 10, TimeUnit.SECONDS)) {
 *     // 执行需要加锁的业务逻辑
 * }
 * </pre>
 */
public interface LockManager {

    /**
     * 获取锁（使用默认超时时间）
     *
     * @param key 锁的键
     * @return 锁上下文
     * @throws TimeoutException 如果获取锁超时
     * @throws InterruptedException 如果线程被中断
     */
    LockContext acquireLock(String key) throws TimeoutException, InterruptedException;

    /**
     * 获取锁（指定超时时间）
     *
     * @param key 锁的键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 锁上下文
     * @throws TimeoutException 如果获取锁超时
     * @throws InterruptedException 如果线程被中断
     */
    LockContext acquireLock(String key, long timeout, TimeUnit unit)
        throws TimeoutException, InterruptedException;

    /**
     * 尝试获取锁（不等待）
     *
     * @param key 锁的键
     * @return 如果成功获取锁返回锁上下文，否则返回空
     */
    LockContext tryLock(String key);

    /**
     * 检查指定键是否已被锁定
     *
     * @param key 锁的键
     * @return 如果已被锁定返回 true
     */
    boolean isLocked(String key);

    /**
     * 释放指定键的锁
     *
     * @param key 锁的键
     */
    void releaseLock(String key);

    /**
     * 清理所有未使用的锁（用于资源清理）
     */
    void cleanup();
}
