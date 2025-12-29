package com.github.zavier.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 速率限制服务
 * 使用滑动窗口算法防止 API 滥用
 */
@Slf4j
@Service
public class RateLimitService {

    // 存储每个用户的请求时间戳记录
    private final Map<Integer, UserRequestHistory> userHistories = new ConcurrentHashMap<>();

    // 配置参数
    private final int maxRequestsPerWindow;
    private final long windowSizeSeconds;

    /**
     * 构造函数，可从配置文件读取参数
     * 默认值：每分钟最多 20 次请求
     */
    public RateLimitService() {
        this.maxRequestsPerWindow = getConfiguredMaxRequests();
        this.windowSizeSeconds = getConfiguredWindowSeconds();

        // 定期清理过期数据
        startCleanupTask();
    }

    /**
     * 检查用户是否允许发起请求
     *
     * @param userId 用户ID
     * @return true 表示允许请求，false 表示超过速率限制
     */
    public boolean allowRequest(Integer userId) {
        if (userId == null) {
            log.warn("[速率限制] 用户ID为空，拒绝请求");
            return false;
        }

        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSizeSeconds;

        // 获取或创建用户请求历史
        UserRequestHistory history = userHistories.computeIfAbsent(
            userId,
            k -> new UserRequestHistory()
        );

        synchronized (history) {
            // 清理窗口外的旧请求记录
            cleanupOldRequests(history, windowStart);

            // 检查是否超过限制
            if (history.requestTimestamps.size() >= maxRequestsPerWindow) {
                log.warn("[速率限制] 用户 {} 超过速率限制，窗口内请求数: {}/{}",
                    userId, history.requestTimestamps.size(), maxRequestsPerWindow);
                return false;
            }

            // 记录当前请求
            history.requestTimestamps.add(now);
            history.lastRequestTime = now;

            log.debug("[速率限制] 用户 {} 请求通过，窗口内请求数: {}/{}",
                userId, history.requestTimestamps.size(), maxRequestsPerWindow);
            return true;
        }
    }

    /**
     * 获取用户在当前窗口内的剩余请求次数
     *
     * @param userId 用户ID
     * @return 剩余请求次数，-1 表示用户没有记录
     */
    public int getRemainingRequests(Integer userId) {
        if (userId == null) {
            return 0;
        }

        UserRequestHistory history = userHistories.get(userId);
        if (history == null) {
            return maxRequestsPerWindow;
        }

        synchronized (history) {
            long now = Instant.now().getEpochSecond();
            long windowStart = now - windowSizeSeconds;
            cleanupOldRequests(history, windowStart);

            return Math.max(0, maxRequestsPerWindow - history.requestTimestamps.size());
        }
    }

    /**
     * 获取速率限制重置时间（秒）
     *
     * @param userId 用户ID
     * @return 距离重置的秒数，-1 表示用户没有记录
     */
    public long getSecondsUntilReset(Integer userId) {
        if (userId == null) {
            return 0;
        }

        UserRequestHistory history = userHistories.get(userId);
        if (history == null || history.requestTimestamps.isEmpty()) {
            return 0;
        }

        synchronized (history) {
            long now = Instant.now().getEpochSecond();
            long oldestRequest = history.requestTimestamps.getFirst();
            long resetTime = oldestRequest + windowSizeSeconds;
            return Math.max(0, resetTime - now);
        }
    }

    /**
     * 重置用户的速率限制（用于管理员操作）
     *
     * @param userId 用户ID
     */
    public void resetUserLimit(Integer userId) {
        if (userId != null) {
            userHistories.remove(userId);
            log.info("[速率限制] 已重置用户 {} 的速率限制", userId);
        }
    }

    /**
     * 清理窗口外的旧请求记录
     */
    private void cleanupOldRequests(UserRequestHistory history, long windowStart) {
        Iterator<Long> iterator = history.requestTimestamps.iterator();
        while (iterator.hasNext()) {
            long timestamp = iterator.next();
            if (timestamp < windowStart) {
                iterator.remove();
            }
        }
    }

    /**
     * 启动定期清理任务
     */
    private void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 每分钟清理一次
                    cleanupInactiveUsers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setName("rate-limit-cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    /**
     * 清理不活跃用户的数据（超过5分钟没有请求）
     */
    private void cleanupInactiveUsers() {
        long now = Instant.now().getEpochSecond();
        long inactiveThreshold = 300; // 5分钟

        userHistories.entrySet().removeIf(entry -> {
            UserRequestHistory history = entry.getValue();
            synchronized (history) {
                if (history.lastRequestTime < now - inactiveThreshold) {
                    log.debug("[速率限制] 清理不活跃用户: {}", entry.getKey());
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 从环境变量读取最大请求数，默认 20
     */
    private int getConfiguredMaxRequests() {
        String value = System.getenv("AI_RATE_LIMIT_MAX_REQUESTS");
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("[速率限制] 无效的配置值 AI_RATE_LIMIT_MAX_REQUESTS: {}, 使用默认值 20", value);
            }
        }
        return 20;
    }

    /**
     * 从环境变量读取窗口大小（秒），默认 60
     */
    private long getConfiguredWindowSeconds() {
        String value = System.getenv("AI_RATE_LIMIT_WINDOW_SECONDS");
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("[速率限制] 无效的配置值 AI_RATE_LIMIT_WINDOW_SECONDS: {}, 使用默认值 60", value);
            }
        }
        return 60;
    }

    /**
     * 用户请求历史记录
     */
    private static class UserRequestHistory {
        // 使用 LinkedList 存储时间戳，便于删除旧记录
        final LinkedList<Long> requestTimestamps = new LinkedList<>();
        long lastRequestTime;
    }
}
