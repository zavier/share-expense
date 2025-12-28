# AI 助手多会话管理功能技术方案

**版本**: 1.0
**日期**: 2025-12-28
**作者**: Claude
**状态**: 设计中

---

## 1. 功能概述

### 1.1 背景

当前 AI 助手仅支持单一会话，用户刷新页面后会丢失 `conversationId`，需要重新开始对话。本功能旨在实现类似 ChatGPT 的多会话管理能力。

### 1.2 核心功能

- **会话列表展示**：左侧边栏显示所有历史会话，移动端采用抽屉式交互
- **创建新会话**：用户输入第一条消息时自动创建新会话
- **切换会话**：点击会话项加载该会话的历史消息
- **删除会话**：物理删除会话及其所有消息
- **重命名会话**：手动编辑会话标题

### 1.3 交互流程

1. 页面加载显示欢迎页，用户输入第一条消息时创建会话
2. 标题默认取第一条消息前 30 字符，用户可点击编辑
3. 点击会话项切换到该会话，加载完整历史消息
4. 使用 `localStorage` 记住当前会话 ID，刷新后自动恢复

---

## 2. 数据库设计

### 2.1 新增表：`ai_chat_session`

```sql
CREATE TABLE ai_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) UNIQUE NOT NULL COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL COMMENT '会话标题',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_conversation (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话元数据表';
```

### 2.2 数据关系

```
ai_chat_session (会话元数据) 1:N ai_conversation (消息记录)
    |                          |
    └── conversation_id ←──────┘
```

### 2.3 设计说明

- `conversation_id`：与现有 `ai_conversation` 表关联，使用 UUID 格式
- `title`：默认从用户第一条消息截取前 30 字符，用户可手动编辑
- 索引优化：`idx_user_created` 支持按用户查询并按时间倒序排列
- 删除策略：采用物理删除，无需软删除字段

---

## 3. 后端 API 设计

### 3.1 新增 API 接口

| 路径 | 方法 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| `/api/ai/sessions` | GET | 获取会话列表 | - | `{success, data: {sessions: [{id, conversationId, title, createdAt, updatedAt}]}}` |
| `/api/ai/sessions` | POST | 创建新会话 | - | `{success, data: {conversationId}}` |
| `/api/ai/sessions/{conversationId}` | DELETE | 删除会话 | - | `{success}` |
| `/api/ai/sessions/{conversationId}/rename` | PUT | 重命名会话 | `{title}` | `{success}` |
| `/api/ai/sessions/{conversationId}/messages` | GET | 获取会话历史消息 | - | `{success, data: {messages: [{role, content, createdAt}]}}` |

### 3.2 现有 API 变更

- `/api/ai/chat`：无需变更，已支持 `conversationId` 参数

### 3.3 Service 层设计

新增 `AiSessionService` 接口：

```java
public interface AiSessionService {
    /**
     * 获取当前用户的所有会话列表
     */
    List<SessionDto> listSessions();

    /**
     * 创建新会话
     */
    SessionDto createSession();

    /**
     * 删除会话及其所有消息
     */
    void deleteSession(String conversationId);

    /**
     * 重命名会话
     */
    void renameSession(String conversationId, String title);

    /**
     * 获取会话的历史消息
     */
    List<MessageDto> getSessionMessages(String conversationId);
}
```

---

## 4. 前端设计

### 4.1 页面布局

```
┌─────────────────────────────────────────────────────┐
│                    Header (保持不变)                   │
├──────────────┬──────────────────────────────────────┤
│              │                                       │
│   侧边栏     │            聊天区域                    │
│              │                                       │
│  [+ 新建]    │     (当前欢迎页/消息列表)               │
│  ─────────   │                                       │
│  📝 会话1    │                                       │
│  📝 会话2    │                                       │
│  📝 会话3    │                                       │
│  ...         │                                       │
│              │                                       │
└──────────────┴──────────────────────────────────────┘
```

**PC 端**：左侧边栏固定宽度 280px，支持折叠
**移动端**：侧边栏默认隐藏，点击菜单按钮从左侧滑出（抽屉式）

### 4.2 组件结构

| 组件 | 职责 |
|------|------|
| `SessionSidebar` | 会话列表侧边栏，包含新建按钮、会话列表项 |
| `SessionItem` | 单个会话项，显示标题、时间，支持点击切换、右键菜单（删除/重命名） |
| `SessionManager` | 会话状态管理，封装 localStorage 操作 |

### 4.3 交互细节

- 当前会话高亮显示
- 会话标题双击或点击编辑图标进入编辑模式
- 删除操作需二次确认
- 切换会话时清空当前消息区，加载新会话历史
- 侧边栏按 `updated_at` 倒序排列（最近更新的在前）

---

## 5. 数据流设计

### 5.1 场景 1：用户发送第一条消息（创建新会话）

```
用户输入消息 → sendMessage()
    ↓
前端：生成新 conversationId (UUID)
    ↓
调用 /api/ai/chat (conversationId: null)
    ↓
后端：AiChatServiceImpl
    ├─ 保存用户消息到 ai_conversation
    ├─ 调用 AI 获取回复
    ├─ 保存 AI 回复
    └─ 从第一条消息提取标题，创建 ai_chat_session 记录
    ↓
返回响应：{conversationId, reply}
    ↓
前端：保存 conversationId 到 localStorage，添加会话到侧边栏
```

### 5.2 场景 2：在已有会话中继续聊天

```
用户在现有会话中输入消息 → sendMessage()
    ↓
前端：从 localStorage 获取当前 conversationId
    ↓
调用 /api/ai/chat (conversationId: "xxx")
    ↓
后端：AiChatServiceImpl
    ├─ 保存用户消息到 ai_conversation (带 conversationId)
    ├─ 查询该会话的历史消息（最近 N 条）构建上下文
    ├─ 调用 AI，携带历史上下文
    ├─ 保存 AI 回复到 ai_conversation
    └─ 更新 ai_chat_session 的 updated_at 时间戳
    ↓
返回响应：{conversationId, reply}
    ↓
前端：追加消息到当前消息区，无需刷新会话列表
```

### 5.3 场景 3：切换会话

```
用户点击会话项
    ↓
前端：更新当前 conversationId
    ↓
调用 /api/ai/sessions/{conversationId}/messages
    ↓
后端：查询 ai_conversation 历史消息
    ↓
返回消息列表
    ↓
前端：清空消息区，渲染历史消息，保存到 localStorage
```

### 5.4 场景 4：删除会话

```
用户确认删除 → deleteSession(conversationId)
    ↓
调用 /api/ai/sessions/{conversationId} (DELETE)
    ↓
后端：事务操作
    ├─ 删除 ai_chat_session 记录
    └─ 删除 ai_conversation 中所有关联消息
    ↓
前端：从侧边栏移除会话项
    ↓
如果删除的是当前会话，清空消息区，显示欢迎页
```

---

## 6. 错误处理

### 6.1 前端错误处理

| 场景 | 处理方式 |
|------|----------|
| 获取会话列表失败 | 显示 Toast 提示，侧边栏显示"加载失败"状态，保留当前会话可用 |
| 创建会话失败 | 显示 Toast 提示，用户可重试 |
| 切换会话失败 | 显示 Toast 提示，保持在原会话 |
| 删除会话失败 | 显示 Toast 提示并说明原因（如"会话不存在"） |
| 加载历史消息失败 | 显示 Toast 提示，消息区显示"加载失败，点击重试" |
| 网络超时 | 显示"网络连接超时，请检查网络后重试" |

### 6.2 后端错误处理

| 异常场景 | HTTP 状态码 | 错误码 |
|----------|-------------|--------|
| 会话不存在 | 404 | `SESSION_NOT_FOUND` |
| 无权访问（会话属于其他用户） | 403 | `FORBIDDEN` |
| 标题为空或过长 | 400 | `INVALID_TITLE` |
| 数据库操作失败 | 500 | `DATABASE_ERROR` |

### 6.3 边界情况处理

- 删除当前会话后，自动清空消息区，显示欢迎页
- 用户删除最后一个会话后，侧边栏显示"暂无会话，开始新对话吧"
- 切换到刚删除的会话时，自动跳转到欢迎页

---

## 7. 测试计划

### 7.1 单元测试

| 测试类 | 覆盖内容 |
|--------|----------|
| `AiSessionServiceTest` | 会话创建、查询、删除、重命名逻辑 |
| `AiSessionRepositoryTest` | 数据访问层 CRUD 操作 |
| `SessionControllerTest` | API 接口请求/响应、参数验证 |

### 7.2 集成测试

| 测试类 | 覆盖内容 |
|--------|----------|
| `AiSessionIntegrationTest` | 完整的会话生命周期流程 |
| `AiChatSessionTest` | 聊天与会话创建的集成 |

### 7.3 前端手动测试用例

1. **会话创建**：输入第一条消息，验证侧边栏出现新会话，标题正确
2. **会话切换**：点击不同会话，验证消息区正确加载历史
3. **会话删除**：删除会话，验证侧边栏和数据库同步删除
4. **会话重命名**：双击标题编辑，验证保存成功
5. **页面刷新**：刷新页面，验证恢复到之前的会话
6. **移动端交互**：验证抽屉式侧边栏展开/收起
7. **异常场景**：网络错误、删除当前会话等边界情况

### 7.4 性能测试

- 会话列表查询响应时间 < 200ms（100 个会话）
- 历史消息加载 < 500ms（100 条消息）

---

## 8. 实施计划

### 8.1 开发任务分解

**阶段 1：数据库与基础设施**
1. 创建 `ai_chat_session` 表的 SQL 迁移脚本
2. 创建 `AiSessionEntity` 实体类
3. 创建 `AiSessionRepository` 接口

**阶段 2：后端 Service 层**
1. 创建 `AiSessionService` 接口
2. 实现 `AiSessionServiceImpl`
3. 修改 `AiChatServiceImpl`：创建会话时同步写入 `ai_chat_session`
4. 创建相关 DTO 类（SessionDto、MessageDto 等）

**阶段 3：后端 Controller 层**
1. 创建 `AiSessionController`，实现 5 个新 API
2. 添加参数验证和异常处理

**阶段 4：前端重构**
1. 创建 `SessionSidebar` 组件（HTML + CSS + JS）
2. 创建 `SessionManager` 状态管理模块
3. 修改主聊天页面，集成侧边栏布局
4. 实现会话切换、创建、删除、重命名交互

**阶段 5：移动端适配**
1. 添加菜单按钮和抽屉动画
2. 响应式布局调整

**阶段 6：测试**
1. 编写单元测试
2. 手动测试各功能场景
3. 修复 Bug

### 8.2 文件清单

**后端新增文件**：
```
share-expense-ai/
├── src/main/java/com/github/zavier/ai/
│   ├── entity/AiSessionEntity.java
│   ├── repository/AiSessionRepository.java
│   ├── service/AiSessionService.java
│   ├── service/impl/AiSessionServiceImpl.java
│   ├── controller/AiSessionController.java
│   └── dto/
│       ├── SessionDto.java
│       ├── MessageDto.java
│       └── SessionListResponse.java
└── src/main/resources/
    └── db/migration/V2025_12_28__create_ai_chat_session.sql
```

**前端修改文件**：
```
start/src/main/resources/static/
├── ai-chat.html (重构)
├── js/
│   ├── session-manager.js (新增)
│   └── session-sidebar.js (新增)
└── css/
    └── session-sidebar.css (新增)
```

---

## 9. 技术决策记录

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 会话标题生成 | 混合方案（默认截取 + 手动编辑） | 兼顾体验和成本 |
| 存储方式 | 新增独立表 | 结构清晰，查询性能好 |
| 移动端交互 | 抽屉式侧边栏 | 符合用户习惯，与主流产品一致 |
| 删除策略 | 物理删除 | 简单彻底，无需恢复功能 |
| 默认行为 | 显示欢迎页，手动创建 | 避免产生空会话 |
| 数量限制 | 不限数量 | 更灵活，暂无需清理策略 |

---

## 10. 附录

### 10.1 当前实现状态

- 消息已存储在 `ai_conversation` 表
- `AiChatService` 已支持 `conversationId` 参数
- 前端已有单页面聊天界面

### 10.2 相关文件

- `share-expense-ai/`: AI 模块代码
- `start/src/main/resources/static/ai-chat.html`: 前端聊天页面
- `CLAUDE.md`: 项目文档

---

**变更历史**：

| 版本 | 日期 | 作者 | 变更说明 |
|------|------|------|----------|
| 1.0 | 2025-12-28 | Claude | 初始版本 |
