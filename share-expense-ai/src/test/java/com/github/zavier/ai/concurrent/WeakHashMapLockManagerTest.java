package com.github.zavier.ai.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WeakHashMapLockManager 单元测试
 * 验证自动内存回收功能
 */
class WeakHashMapLockManagerTest {

    private LockManager lockManager;

    @BeforeEach
    void setUp() {
        lockManager = new WeakHashMapLockManager();
    }

    @Test
    void testBasicLockAcquire() throws Exception {
        try (LockContext context = lockManager.acquireLock("test-key")) {
            assertTrue(context.isAcquired());
            assertTrue(lockManager.isLocked("test-key"));
        }

        // 释放后不再锁定
        assertFalse(lockManager.isLocked("test-key"));
    }

    @Test
    void testAutomaticGarbageCollection() throws Exception {
        // 创建大量锁
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String key = "gc-test-" + i;
            keys.add(key);
            LockContext context = lockManager.acquireLock(key);
            context.close();
        }

        // 获取统计
        WeakHashMapLockManager weakLockManager = (WeakHashMapLockManager) lockManager;
        WeakHashMapLockManager.LockStatistics stats1 = weakLockManager.getStatistics();

        System.out.println("Before GC: " + stats1);

        // 清空引用
        keys.clear();

        // 建议 GC
        System.gc();
        Thread.sleep(100);

        // 清理已被回收的锁
        lockManager.cleanup();

        WeakHashMapLockManager.LockStatistics stats2 = weakLockManager.getStatistics();
        System.out.println("After GC: " + stats2);

        // 验证锁数量减少（GC 自动回收）
        assertTrue(stats2.totalLocks() < stats1.totalLocks(),
            "GC should have collected some locks");
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    String key = "concurrent-" + (index % 3); // 3个不同的键
                    LockContext context = lockManager.acquireLock(key);
                    try {
                        successCount.incrementAndGet();
                        Thread.sleep(10);
                    } finally {
                        context.close();
                    }
                } catch (Exception e) {
                    fail("Unexpected exception: " + e);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(threadCount, successCount.get());

        executor.shutdown();
    }

    @Test
    void testReentrantLock() throws Exception {
        String key = "reentrant-key";

        try (LockContext context1 = lockManager.acquireLock(key)) {
            assertTrue(context1.isAcquired());

            // 同一线程可以再次获取
            try (LockContext context2 = lockManager.acquireLock(key)) {
                assertTrue(context2.isAcquired());
            }

            assertTrue(lockManager.isLocked(key));
        }

        // 全部释放后才解锁
        assertFalse(lockManager.isLocked(key));
    }

    @Test
    void testTryLock() {
        LockContext context = lockManager.tryLock("try-key");

        assertTrue(context.isAcquired());
        assertTrue(lockManager.isLocked("try-key"));

        context.close();
        assertFalse(lockManager.isLocked("try-key"));
    }

    @Test
    void testTimeout() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);

        executor.submit(() -> {
            try {
                LockContext context = lockManager.acquireLock("timeout-key");
                started.countDown();
                assertTrue(done.await(2, TimeUnit.SECONDS));
                context.close();
            } catch (Exception e) {
                fail("Unexpected exception: " + e);
            }
        });

        assertTrue(started.await(1, TimeUnit.SECONDS));

        // 应该超时
        assertThrows(java.util.concurrent.TimeoutException.class, () -> {
            lockManager.acquireLock("timeout-key", 100, TimeUnit.MILLISECONDS);
        });

        done.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testStatistics() throws Exception {
        WeakHashMapLockManager weakLockManager = (WeakHashMapLockManager) lockManager;

        WeakHashMapLockManager.LockStatistics stats = weakLockManager.getStatistics();

        assertNotNull(stats);
        assertEquals(0, stats.totalLocks());
        assertEquals(0, stats.activeLocks());
        assertEquals(0, stats.unusedLocks());

        // 创建几个锁
        try (LockContext c1 = lockManager.acquireLock("key1")) {
            stats = weakLockManager.getStatistics();
            assertTrue(stats.totalLocks() >= 1);
            assertTrue(stats.activeLocks() >= 1);
        }
    }

    @Test
    void testManualRelease() throws Exception {
        lockManager.acquireLock("manual-key");
        assertTrue(lockManager.isLocked("manual-key"));

        lockManager.releaseLock("manual-key");
        assertFalse(lockManager.isLocked("manual-key"));
    }

    @Test
    void testMultipleKeys() throws Exception {
        try (LockContext c1 = lockManager.acquireLock("key1");
             LockContext c2 = lockManager.acquireLock("key2")) {

            assertTrue(c1.isAcquired());
            assertTrue(c2.isAcquired());
            assertTrue(lockManager.isLocked("key1"));
            assertTrue(lockManager.isLocked("key2"));
        }

        assertFalse(lockManager.isLocked("key1"));
        assertFalse(lockManager.isLocked("key2"));
    }
}
