package com.github.zavier.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 速率限制服务测试类
 */
@DisplayName("速率限制服务测试")
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    @DisplayName("应该允许在限制内的请求")
    void testAllowRequestsUnderLimit() {
        Integer userId = 1;

        // 默认配置：每分钟20次请求
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.allowRequest(userId),
                "第 " + (i + 1) + " 次请求应该被允许");
        }
    }

    @Test
    @DisplayName("应该拒绝超过限制的请求")
    void testRejectRequestsOverLimit() {
        Integer userId = 2;

        // 发送20次请求（达到限制）
        for (int i = 0; i < 20; i++) {
            rateLimitService.allowRequest(userId);
        }

        // 第21次请求应该被拒绝
        assertFalse(rateLimitService.allowRequest(userId),
            "超过限制的请求应该被拒绝");
    }

    @Test
    @DisplayName("不同用户的速率限制应该独立")
    void testIndependentRateLimitsPerUser() {
        Integer user1 = 3;
        Integer user2 = 4;

        // 用户1达到限制
        for (int i = 0; i < 20; i++) {
            rateLimitService.allowRequest(user1);
        }
        assertFalse(rateLimitService.allowRequest(user1),
            "用户1应该被限制");

        // 用户2应该不受影响
        assertTrue(rateLimitService.allowRequest(user2),
            "用户2应该被允许");
    }

    @Test
    @DisplayName("应该正确计算剩余请求次数")
    void testGetRemainingRequests() {
        Integer userId = 5;

        assertEquals(20, rateLimitService.getRemainingRequests(userId),
            "初始剩余请求数应该是20");

        // 发送5次请求
        for (int i = 0; i < 5; i++) {
            rateLimitService.allowRequest(userId);
        }

        assertEquals(15, rateLimitService.getRemainingRequests(userId),
            "发送5次请求后剩余请求数应该是15");
    }

    @Test
    @DisplayName("应该拒绝null用户ID的请求")
    void testRejectNullUserId() {
        assertFalse(rateLimitService.allowRequest(null),
            "null用户ID的请求应该被拒绝");
    }

    @Test
    @DisplayName("应该正确处理重置用户限制")
    void testResetUserLimit() {
        Integer userId = 6;

        // 达到限制
        for (int i = 0; i < 20; i++) {
            rateLimitService.allowRequest(userId);
        }
        assertFalse(rateLimitService.allowRequest(userId));

        // 重置后应该允许请求
        rateLimitService.resetUserLimit(userId);
        assertTrue(rateLimitService.allowRequest(userId),
            "重置后请求应该被允许");
    }

    @Test
    @DisplayName("重置不存在的用户不应该抛出异常")
    void testResetNonExistentUser() {
        // 应该不抛出异常
        assertDoesNotThrow(() -> rateLimitService.resetUserLimit(99999));
    }

    @Test
    @DisplayName("获取不存在用户的剩余请求数应该返回最大值")
    void testGetRemainingRequestsForNonExistentUser() {
        assertEquals(20, rateLimitService.getRemainingRequests(99999),
            "不存在用户的剩余请求数应该返回最大值");
    }

    @Test
    @DisplayName("获取不存在用户的重置时间应该返回0")
    void testGetSecondsUntilResetForNonExistentUser() {
        assertEquals(0, rateLimitService.getSecondsUntilReset(99999),
            "不存在用户的重置时间应该返回0");
    }

    @Test
    @DisplayName("并发请求应该正确处理")
    void testConcurrentRequests() throws InterruptedException {
        Integer userId = 7;
        int threadCount = 10;
        int requestsPerThread = 2;
        int totalRequests = threadCount * requestsPerThread;

        Thread[] threads = new Thread[threadCount];
        int[] successCount = {0};

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    if (rateLimitService.allowRequest(userId)) {
                        synchronized (successCount) {
                            successCount[0]++;
                        }
                    }
                    try {
                        Thread.sleep(10); // 短暂延迟模拟真实场景
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 总共20次请求应该全部成功
        assertEquals(totalRequests, successCount[0],
            "所有并发请求都应该成功");

        // 第21次请求应该被拒绝
        assertFalse(rateLimitService.allowRequest(userId),
            "超过限制的并发请求应该被拒绝");
    }

    @Test
    @DisplayName("新用户应该有完整的请求配额")
    void testNewUserHasFullQuota() {
        Integer newUserId = 8;

        // 新用户应该能够发送完整配额的请求
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.allowRequest(newUserId),
                "新用户应该能发送第 " + (i + 1) + " 次请求");
        }
    }

    @Test
    @DisplayName("应该正确返回重置时间")
    void testSecondsUntilReset() {
        Integer userId = 9;

        // 发送一次请求
        rateLimitService.allowRequest(userId);

        long secondsUntilReset = rateLimitService.getSecondsUntilReset(userId);

        // 重置时间应该在0到窗口大小之间
        assertTrue(secondsUntilReset >= 0 && secondsUntilReset <= 60,
            "重置时间应该在0到60秒之间");
    }
}
