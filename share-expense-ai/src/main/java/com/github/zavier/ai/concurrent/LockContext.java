package com.github.zavier.ai.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Lock;

/**
 * 锁上下文，支持 try-with-resources 模式自动释放锁
 *
 * 使用示例：
 * <pre>
 * try (LockContext context = lockManager.acquireLock("key")) {
 *     // 执行业务逻辑
 * } // 自动释放锁
 * </pre>
 */
@Slf4j
public class LockContext implements AutoCloseable {

    private final String key;
    private final Lock lock;
    private final LockManager lockManager;
    private final boolean acquired;
    private volatile boolean closed = false;

    /**
     * 创建锁上下文
     *
     * @param key 锁的键
     * @param lock 实际的锁对象
     * @param lockManager 锁管理器
     * @param acquired 是否成功获取锁
     */
    public LockContext(String key, Lock lock, LockManager lockManager, boolean acquired) {
        this.key = key;
        this.lock = lock;
        this.lockManager = lockManager;
        this.acquired = acquired;
    }

    /**
     * 检查是否成功获取锁
     *
     * @return 如果成功获取锁返回 true
     */
    public boolean isAcquired() {
        return acquired && !closed;
    }

    /**
     * 检查锁是否已释放
     *
     * @return 如果锁已释放返回 true
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * 获取锁的键
     *
     * @return 锁的键
     */
    public String getKey() {
        return key;
    }

    @Override
    public void close() {
        if (closed) {
            log.warn("LockContext already closed for key: {}", key);
            return;
        }

        try {
            if (acquired && lock != null) {
                log.debug("Releasing lock for key: {}", key);
                lock.unlock();
            }
        } finally {
            closed = true;
            // 通知锁管理器清理（如果支持）
            if (lockManager instanceof CleanupAware) {
                ((CleanupAware) lockManager).onLockReleased(key);
            }
        }
    }

    /**
     * 清理感知接口
     */
    interface CleanupAware {
        void onLockReleased(String key);
    }
}
