package com.github.zavier.ai.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 WeakHashMap 的本地锁管理器
 * 自动清理未被引用的锁，防止内存泄漏
 *
 * 工作原理：
 * - 使用 WeakReference 包装锁对象
 * - 当锁的键不再被外部引用时，GC 会自动回收
 * - 定期清理被回收的锁对象
 */
@Slf4j
public class WeakHashMapLockManager implements LockManager, LockContext.CleanupAware {

    // 使用 ConcurrentHashMap 存储 WeakReference，当键不再被引用时，GC 会自动回收
    private final Map<String, WeakReference<Lock>> locks = new ConcurrentHashMap<>();

    // 跟踪锁的使用情况
    private final Map<String, Integer> lockHoldCount = new ConcurrentHashMap<>();

    // 默认超时时间：10秒
    private static final long DEFAULT_TIMEOUT_SECONDS = 10;

    @Override
    public LockContext acquireLock(String key) throws TimeoutException, InterruptedException {
        return acquireLock(key, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public LockContext acquireLock(String key, long timeout, TimeUnit unit)
            throws TimeoutException, InterruptedException {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Lock key cannot be null or empty");
        }

        // 清理已回收的锁
        cleanUpGarbageCollectedLocks();

        // 获取或创建锁
        Lock lock = getOrCreateLock(key);

        // 增加持有计数
        lockHoldCount.merge(key, 1, Integer::sum);

        boolean acquired = lock.tryLock(timeout, unit);

        if (!acquired) {
            // 获取锁失败，减少计数
            lockHoldCount.merge(key, -1, Integer::sum);
            throw new TimeoutException("Failed to acquire lock for key: " + key +
                " within " + timeout + " " + unit);
        }

        log.debug("Successfully acquired lock for key: {}", key);
        return new LockContext(key, lock, this, true);
    }

    @Override
    public LockContext tryLock(String key) {
        // 清理已回收的锁
        cleanUpGarbageCollectedLocks();

        Lock lock = getOrCreateLock(key);

        boolean acquired = lock.tryLock();

        if (acquired) {
            lockHoldCount.merge(key, 1, Integer::sum);
            log.debug("Successfully acquired lock (tryLock) for key: {}", key);
            return new LockContext(key, lock, this, true);
        } else {
            return new LockContext(key, lock, this, false);
        }
    }

    @Override
    public boolean isLocked(String key) {
        WeakReference<Lock> lockRef = locks.get(key);
        if (lockRef == null) {
            return false;
        }

        Lock lock = lockRef.get();
        if (lock == null) {
            // 锁已被回收，清理
            locks.remove(key);
            lockHoldCount.remove(key);
            return false;
        }

        if (lock instanceof ReentrantLock reentrantLock) {
            return reentrantLock.isLocked();
        }

        return false;
    }

    @Override
    public void releaseLock(String key) {
        WeakReference<Lock> lockRef = locks.get(key);
        if (lockRef == null) {
            return;
        }

        Lock lock = lockRef.get();
        if (lock == null) {
            // 锁已被回收，清理
            locks.remove(key);
            lockHoldCount.remove(key);
            return;
        }

        if (lock instanceof ReentrantLock reentrantLock) {
            if (reentrantLock.isHeldByCurrentThread()) {
                reentrantLock.unlock();
                lockHoldCount.merge(key, -1, Integer::sum);
                log.debug("Manually released lock for key: {}", key);

                // 如果持有计数为0，可以移除锁对象（让GC回收）
                Integer count = lockHoldCount.get(key);
                if (count != null && count <= 0) {
                    locks.remove(key);
                    lockHoldCount.remove(key);
                    log.debug("Removed lock with zero hold count for key: {}", key);
                }
            }
        }
    }

    @Override
    public void cleanup() {
        cleanUpGarbageCollectedLocks();
    }

    @Override
    public void onLockReleased(String key) {
        // LockContext 关闭时调用
        Integer count = lockHoldCount.get(key);
        if (count != null && count <= 0) {
            // 没有持有者了，可以移除
            locks.remove(key);
            lockHoldCount.remove(key);
            log.debug("Removed unreferenced lock for key: {}", key);
        }
    }

    /**
     * 获取或创建锁
     */
    private Lock getOrCreateLock(String key) {
        while (true) {
            WeakReference<Lock> lockRef = locks.get(key);
            if (lockRef != null) {
                Lock lock = lockRef.get();
                if (lock != null) {
                    return lock;
                }
                // 锁已被回收，清理并重新创建
                locks.remove(key);
            }

            // 创建新锁
            Lock newLock = new ReentrantLock();
            WeakReference<Lock> newRef = new WeakReference<>(newLock);

            // 使用 putIfAbsent 确保原子性
            WeakReference<Lock> existing = locks.putIfAbsent(key, newRef);
            if (existing == null) {
                // 成功创建
                log.debug("Created new lock for key: {}", key);
                return newLock;
            }

            // 其他线程已经创建了，重新尝试
            Lock existingLock = existing.get();
            if (existingLock != null) {
                return existingLock;
            }
            // 其他线程的锁已被回收，重试
        }
    }

    /**
     * 清理已被 GC 回收的锁
     * 这是 WeakHashMap 能工作的关键
     */
    private void cleanUpGarbageCollectedLocks() {
        int removed = 0;

        for (Map.Entry<String, WeakReference<Lock>> entry : locks.entrySet()) {
            String key = entry.getKey();
            WeakReference<Lock> lockRef = entry.getValue();

            if (lockRef.get() == null) {
                locks.remove(key);
                lockHoldCount.remove(key);
                removed++;
                log.debug("Cleaned up garbage collected lock for key: {}", key);
            }
        }

        if (removed > 0) {
            log.debug("Cleaned up {} garbage collected locks", removed);
        }
    }

    /**
     * 获取统计信息
     */
    public LockStatistics getStatistics() {
        cleanUpGarbageCollectedLocks();

        int totalLocks = locks.size();
        int activeLocks = 0;
        int unusedLocks = 0;

        for (Map.Entry<String, WeakReference<Lock>> entry : locks.entrySet()) {
            Lock lock = entry.getValue().get();
            if (lock != null) {
                if (lock instanceof ReentrantLock reentrantLock) {
                    if (reentrantLock.isLocked()) {
                        activeLocks++;
                    } else {
                        unusedLocks++;
                    }
                }
            }
        }

        return new LockStatistics(totalLocks, activeLocks, unusedLocks);
    }

    /**
     * 锁统计信息
     */
    public record LockStatistics(
        int totalLocks,
        int activeLocks,
        int unusedLocks
    ) {
        @Override
        public String toString() {
            return String.format("LockStatistics{total=%d, active=%d, unused=%d}",
                totalLocks, activeLocks, unusedLocks);
        }
    }
}
