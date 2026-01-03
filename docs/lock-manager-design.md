# 锁管理器工具类设计文档

## 概述

锁管理器工具类提供统一的锁管理能力，支持本地锁和分布式锁，并支持 try-with-resources 自动释放。

## 设计目标

1. **统一接口**：提供 `LockManager` 接口，支持多种实现
2. **自动释放**：使用 `AutoCloseable` 支持 try-with-resources
3. **易于扩展**：可轻松切换到分布式锁
4. **防止内存泄漏**：自动清理未使用的锁对象
5. **线程安全**：所有操作都是线程安全的

## 架构设计

```
LockManager (接口)
    ├── LocalLockManager (本地锁实现)
    │   └── 基于 ReentrantLock + WeakHashMap
    └── RedisLockManager (分布式锁实现)
        └── 基于 Redis + Lua 脚本
```

## 核心组件

### 1. LockManager 接口

定义统一的锁管理接口：

```java
public interface LockManager {
    // 获取锁（默认超时）
    LockContext acquireLock(String key) throws TimeoutException, InterruptedException;

    // 获取锁（指定超时）
    LockContext acquireLock(String key, long timeout, TimeUnit unit)
        throws TimeoutException, InterruptedException;

    // 尝试获取锁（不等待）
    LockContext tryLock(String key);

    // 检查是否锁定
    boolean isLocked(String key);

    // 手动释放锁
    void releaseLock(String key);

    // 清理未使用的锁
    void cleanup();
}
```

### 2. LockContext 类

锁上下文，支持自动释放：

```java
public class LockContext implements AutoCloseable {
    private final String key;
    private final Lock lock;
    private final boolean acquired;

    // 是否成功获取锁
    public boolean isAcquired()

    // 自动释放锁
    @Override
    public void close()
}
```

### 3. LocalLockManager 实现

基于 `ReentrantLock` 和 `WeakHashMap` 的本地锁管理器：

**特性：**
- 使用 `WeakHashMap` 自动清理未被引用的锁
- 跟踪锁使用情况，支持手动清理
- 可重入锁支持
- 提供统计信息

**优点：**
- 无需额外依赖
- 性能高（本地操作）
- 自动防止内存泄漏

**缺点：**
- 只适用于单实例部署
- 多实例间无法同步

### 4. RedisLockManager 实现

基于 Redis 的分布式锁管理器：

**特性：**
- 分布式锁，支持多实例
- 自动续期（看门狗机制）
- 防止死锁（锁超时自动释放）
- Lua 脚本保证原子性

**优点：**
- 支持多实例部署
- 可靠性高

**缺点：**
- 依赖 Redis
- 性能略低于本地锁

## 使用示例

### 基本使用

```java
@Autowired
private LockManager lockManager;

public void doSomething() {
    // 使用 try-with-resources 自动释放
    try (LockContext context = lockManager.acquireLock("my-key")) {
        // 执行需要加锁的业务逻辑
        // ...
    } // 自动释放锁
}
```

### 指定超时时间

```java
try (LockContext context = lockManager.acquireLock("my-key", 5, TimeUnit.SECONDS)) {
    // 业务逻辑
}
```

### 尝试获取锁（不等待）

```java
LockContext context = lockManager.tryLock("my-key");
if (context.isAcquired()) {
    try {
        // 获取锁成功，执行业务逻辑
    } finally {
        context.close();
    }
} else {
    // 获取锁失败，执行降级逻辑
}
```

### 检查锁状态

```java
if (lockManager.isLocked("my-key")) {
    // 锁已被持有
}
```

### 手动释放锁

```java
lockManager.releaseLock("my-key");
```

### 定期清理

```java
@Scheduled(fixedRate = 300000) // 每5分钟
public void cleanupLocks() {
    lockManager.cleanup();
}
```

### 获取统计信息（LocalLockManager）

```java
LocalLockManager localLockManager = (LocalLockManager) lockManager;
LocalLockManager.LockStatistics stats = localLockManager.getStatistics();

System.out.println("Total locks: " + stats.totalLocks());
System.out.println("Active locks: " + stats.activeLocks());
System.out.println("Unused locks: " + stats.unusedLocks());
```

## 配置

### application.properties

```properties
# 默认使用本地锁
app.lock.type=local

# 或使用 Redis 分布式锁
app.lock.type=redis
```

### 依赖配置

**使用 RedisLockManager 时需要：**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

## 在 CachedSuggestionService 中的应用

### 修改前（直接使用 ReentrantLock）

```java
private final ConcurrentHashMap<String, Lock> conversationLocks = new ConcurrentHashMap<>();

public void generateSuggestions() {
    Lock lock = conversationLocks.computeIfAbsent(key, k -> new ReentrantLock());

    try {
        if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
            try {
                // 业务逻辑
            } finally {
                lock.unlock();
            }
        }
    } catch (InterruptedException e) {
        // 异常处理
    }
}
```

**问题：**
- ❌ 锁对象永不清理，内存泄漏
- ❌ 手动管理锁释放，容易出错
- ❌ 无法扩展为分布式锁

### 修改后（使用 LockManager）

```java
@Autowired
private LockManager lockManager;

public void generateSuggestions() {
    try (LockContext context = lockManager.acquireLock(key, 100, TimeUnit.MILLISECONDS)) {
        // 业务逻辑
    } catch (TimeoutException e) {
        // 获取锁超时
    } catch (InterruptedException e) {
        // 线程中断
    }
}
```

**优点：**
- ✅ 自动释放锁，不会遗漏
- ✅ 自动清理未使用的锁对象
- ✅ 代码更简洁
- ✅ 易于切换到分布式锁

## 迁移指南

### 从旧的锁管理方式迁移

**步骤 1：注入 LockManager**

```java
// 旧代码
private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

// 新代码
@Autowired
private LockManager lockManager;
```

**步骤 2：替换锁获取和释放**

```java
// 旧代码
Lock lock = locks.get(key);
try {
    lock.lock();
    // 业务逻辑
} finally {
    lock.unlock();
}

// 新代码
try (LockContext context = lockManager.acquireLock(key)) {
    // 业务逻辑
}
```

**步骤 3：删除锁清理代码**

```java
// 删除这些
locks.clear();
locks.remove(key);
```

### 切换到分布式锁

**步骤 1：添加 Redis 依赖**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**步骤 2：修改配置**

```properties
# application.properties
app.lock.type=redis

# Redis 配置
spring.redis.host=localhost
spring.redis.port=6379
```

**步骤 3：无需修改代码**

LockManager 接口不变，代码无需修改！

## 性能对比

| 操作 | LocalLockManager | RedisLockManager |
|------|------------------|------------------|
| 获取锁 | ~0.1ms | ~1-5ms |
| 释放锁 | ~0.01ms | ~1-3ms |
| 内存占用 | 低（自动清理） | 低（Redis） |
| 适用场景 | 单实例 | 多实例 |

## 最佳实践

### 1. 优先使用 try-with-resources

```java
// ✅ 推荐
try (LockContext context = lockManager.acquireLock(key)) {
    // 业务逻辑
}

// ❌ 不推荐
LockContext context = lockManager.acquireLock(key);
try {
    // 业务逻辑
} finally {
    context.close();
}
```

### 2. 设置合理的超时时间

```java
// 根据业务逻辑调整超时
lockManager.acquireLock(key, 5, TimeUnit.SECONDS);
```

### 3. 定期清理（可选）

```java
@Scheduled(fixedRate = 300000) // 每5分钟
public void scheduledCleanup() {
    lockManager.cleanup();
}
```

### 4. 处理超时异常

```java
try (LockContext context = lockManager.acquireLock(key, timeout, unit)) {
    // 业务逻辑
} catch (TimeoutException e) {
    // 获取锁超时，执行降级逻辑
    log.warn("Failed to acquire lock, using fallback");
    return getDefaultResult();
}
```

### 5. 避免锁嵌套

```java
// ❌ 避免嵌套锁（可能导致死锁）
try (LockContext context1 = lockManager.acquireLock("key1")) {
    try (LockContext context2 = lockManager.acquireLock("key2")) {
        // 业务逻辑
    }
}

// ✅ 使用单个锁
try (LockContext context = lockManager.acquireLock("key1:key2")) {
    // 业务逻辑
}
```

## 扩展性

### 自定义锁管理器

可以实现 `LockManager` 接口来自定义锁管理器：

```java
public class CustomLockManager implements LockManager {

    @Override
    public LockContext acquireLock(String key, long timeout, TimeUnit unit) {
        // 自定义实现
    }

    // ... 其他方法
}
```

### 配置类中注册

```java
@Configuration
public class LockManagerConfig {

    @Bean
    @ConditionalOnProperty(name = "app.lock.type", havingValue = "custom")
    public LockManager customLockManager() {
        return new CustomLockManager();
    }
}
```

## 总结

锁管理器工具类提供了：

✅ **统一的接口**：易于切换实现
✅ **自动释放**：防止资源泄漏
✅ **防止内存泄漏**：自动清理未使用的锁
✅ **易于扩展**：支持自定义实现
✅ **线程安全**：所有操作都是线程安全的

这个工具类不仅解决了当前代码中的内存泄漏问题，还为未来的分布式部署提供了平滑的迁移路径。
