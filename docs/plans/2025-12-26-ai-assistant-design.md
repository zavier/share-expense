# AI 记账助手设计方案

**日期**: 2025-12-26
**分支**: feature/ai-assistant
**目标**: 接入 Spring AI，支持通过自然语言调用 API 进行记账操作

---

## 1. 概述

### 1.1 目标用户
普通用户，希望通过简单的对话完成日常记账操作。

### 1.2 核心价值
- 降低使用门槛：无需学习界面操作，直接对话即可记账
- 提升操作效率：一条消息完成多个操作
- 友好交互：支持多轮对话和上下文理解

### 1.3 技术选型
- **AI 模型**: OpenAI GPT-4o / GPT-4o-mini
- **框架**: Spring AI 1.0.0+
- **交互方式**: Function Calling (函数调用)

---

## 2. 功能范围

### 2.1 支持的操作（核心功能）
| 操作 | 说明 |
|------|------|
| 创建项目 | 创建新的费用分摊项目，同时添加成员 |
| 添加成员 | 向现有项目添加新成员 |
| 添加费用记录 | 记录一笔支出，指定付款人和消费人员 |
| 查询结算 | 查询项目的费用结算情况 |

### 2.2 不支持的操作
- 删除项目/记录（避免误操作）
- 修改记录（数据安全考虑）
- 导出数据（复杂度较高）

---

## 3. 系统架构

### 3.1 整体架构

```
用户输入
    ↓
AiChatController
    ↓
Spring AI (OpenAI) ←→ Function Registry
    ↓
Function Call (函数调用)
    ↓
【确认机制】→ 用户确认
    ↓
业务逻辑执行 (ProjectService)
    ↓
结果返回 → AI 生成回复
    ↓
展示给用户
```

### 3.2 模块结构

**新增模块: share-expense-ai**

```
share-expense-ai/
├── src/main/java/com/github/zavier/ai/
│   ├── AiChatController.java          # 聊天 API
│   ├── AiChatService.java             # 对话服务
│   ├── AiFunctionRegistry.java        # 函数注册中心
│   ├── function/                       # 函数实现
│   │   ├── CreateProjectFunction.java
│   │   ├── AddMembersFunction.java
│   │   ├── AddExpenseRecordFunction.java
│   │   └── GetSettlementFunction.java
│   ├── conversation/                   # 对话管理
│   │   ├── ConversationService.java
│   │   └── ConversationRepository.java
│   └── dto/                            # 数据传输对象
│       ├── AiChatRequest.java
│       ├── AiChatResponse.java
│       ├── PendingAction.java
│       └── ChatMessage.java
└── src/main/resources/
    └── ai-prompts.txt                  # System prompt
```

---

## 4. AI 函数定义

### 4.1 函数列表

```java
// 1. 创建项目
@Function(name = "createProject", description = "创建一个新的费用分摊项目")
ProjectDTO createProject(
    @Param(description = "项目名称") String name,
    @Param(description = "项目描述") String description,
    @Param(description = "成员列表") List<String> members
)

// 2. 添加成员
@Function(name = "addMembers", description = "向项目添加成员")
Void addMembers(
    @Param(description = "项目ID") Integer projectId,
    @Param(description = "成员名称列表") List<String> memberNames
)

// 3. 添加费用记录
@Function(name = "addExpenseRecord", description = "添加一笔费用记录")
ExpenseRecordDTO addExpenseRecord(
    @Param(description = "项目ID") Integer projectId,
    @Param(description = "付款人") String payer,
    @Param(description = "金额") BigDecimal amount,
    @Param(description = "费用类型") String expenseType,
    @Param(description = "消费日期") LocalDate payDate,
    @Param(description = "参与消费的成员列表") List<String> consumers,
    @Param(description = "备注") String remark
)

// 4. 查询结算
@Function(name = "getSettlement", description = "查询项目结算情况")
SettlementDTO getSettlement(
    @Param(description = "项目ID") Integer projectId
)
```

### 4.2 函数注册

```java
@Component
public class AiFunctionRegistry {
    private final Map<String, FunctionDefinition> functions = new HashMap<>();

    @PostConstruct
    public void registerFunctions() {
        register(createProjectFunction);
        register(addMembersFunction);
        register(addExpenseRecordFunction);
        register(getSettlementFunction);
    }

    public List<FunctionDefinition> getAllFunctions() {
        return new ArrayList<>(functions.values());
    }
}
```

---

## 5. API 接口设计

### 5.1 接口列表

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai/chat` | POST | 发送消息，AI 解析意图 |
| `/api/ai/confirm` | POST | 确认执行操作 |
| `/api/ai/cancel` | POST | 取消待执行的操作 |
| `/api/ai/history` | GET | 获取对话历史 |

### 5.2 请求/响应结构

```java
// 聊天请求
public class AiChatRequest {
    private String message;          // 用户输入的消息
    private String conversationId;   // 会话ID（可选）
}

// 聊天响应
public class AiChatResponse {
    private String conversationId;   // 会话ID
    private String reply;            // AI回复文本
    private PendingAction pendingAction;  // 待确认的操作
}

// 待确认的操作
public class PendingAction {
    private String actionType;       // createProject, addExpenseRecord等
    private String description;      // 操作描述（自然语言）
    private Map<String, Object> params;  // 提取的参数
}

// 确认请求
public class ConfirmRequest {
    private String conversationId;
    private String actionId;         // 操作ID
}

// 取消请求
public class CancelRequest {
    private String conversationId;
}
```

---

## 6. 交互流程

### 6.1 正常流程示例

```
用户: "帮我创建一个西藏旅游的项目，成员有张三、李四、王五"

→ AI 解析: createProject(name="西藏旅游", members=["张三","李四","王五"])

→ 前端展示: "好的，我来帮您创建西藏旅游项目，成员包括：张三、李四、王五。"
            [确认执行] [取消]

→ 用户点击: [确认执行]

→ 后端执行: projectService.createProject()

→ AI 生成回复: "项目创建成功！项目ID是123，您现在可以开始记录费用了。"
```

### 6.2 带确认的流程

```
用户: "今天午饭张三付了80元，我们四个人平摊"

→ AI 询问: "请问是哪个项目？"

用户: "西藏旅游项目"

→ AI 解析: addExpenseRecord(projectId=123, payer="张三", amount=80, ...)

→ 前端展示: "好的，我来添加费用记录：张三支付午餐80元，四个人平摊。"
            [确认执行] [取消]

→ 用户确认 → 执行 → 返回结果
```

---

## 7. 前端设计

### 7.1 页面结构

使用 amis 框架实现聊天界面：

```json
{
  "type": "page",
  "title": "AI 记账助手",
  "body": {
    "type": "flex",
    "direction": "column",
    "items": [
      {
        "type": "chat-messages",
        "name": "messages",
        "source": "${messages}"
      },
      {
        "type": "form",
        "body": {
          "type": "input-text",
          "name": "message",
          "placeholder": "例如：今天午饭张三付了80元，我们四个人平摊",
          "submitOnChange": true
        }
      }
    ]
  }
}
```

### 7.2 消息格式

```json
{
  "messages": [
    {
      "id": "msg_001",
      "role": "user",
      "content": "帮我创建一个西藏旅游的项目",
      "timestamp": "2025-12-26 10:30:00"
    },
    {
      "id": "msg_002",
      "role": "assistant",
      "content": "好的，我来帮您创建西藏旅游项目，成员包括：张三、李四",
      "timestamp": "2025-12-26 10:30:02",
      "pendingAction": {
        "actionId": "action_001",
        "actionType": "createProject",
        "description": "创建项目'西藏旅游'，添加成员：张三、李四"
      }
    }
  ]
}
```

### 7.3 快捷指令提示

```
💡 您可以这样说：
• "创建项目'周末聚餐'，成员有小明、小红"
• "记录今天午餐，小李付了50元，我们三个人平摊"
• "查询项目123的结算情况"
• "给项目5添加成员：小王"
```

---

## 8. 配置管理

### 8.1 Spring AI 配置

```properties
# OpenAI 配置
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.base-url=https://api.openai.com

# 代理配置（可选）
# spring.ai.openai.base-url=http://localhost:7890
```

### 8.2 应用配置

```properties
# AI 对话配置
app.ai.chat.enabled=true
app.ai.chat.max-history-rounds=10
app.ai.chat.confirm-timeout-minutes=5
app.ai.chat.rate-limit-per-minute=20
```

---

## 9. 数据库设计

### 9.1 对话历史表

```sql
CREATE TABLE ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    pending_action JSON COMMENT '待确认的操作',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_conversation (conversation_id),
    INDEX idx_user (user_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话历史';
```

---

## 10. 安全与错误处理

### 10.1 安全措施

| 措施 | 说明 |
|------|------|
| 用户权限隔离 | AI 调用业务函数时自动注入当前用户 ID |
| 参数验证 | 所有 AI 提取的参数必须经过业务层校验 |
| 限流保护 | 每用户每分钟最多 20 条请求 |
| 审计日志 | 记录所有 AI 对话和操作执行日志 |
| 功能开关 | 支持动态开启/关闭 AI 功能 |

### 10.2 错误处理

| 场景 | 处理方式 |
|------|----------|
| AI 无法理解意图 | 返回提示："抱歉，我没太理解。您可以尝试说：创建项目/添加费用/查询结算" |
| 参数提取不完整 | AI 主动询问缺失信息 |
| 业务逻辑执行失败 | 返回具体错误信息 |
| 用户超时未确认 | pendingAction 5分钟后失效 |
| 网络请求失败 | 显示重试按钮 |

---

## 11. 实施计划

### 11.1 开发步骤

1. **创建分支和模块**
   - 创建 `feature/ai-assistant` 分支
   - 新增 `share-expense-ai` 模块
   - 添加 Spring AI 依赖

2. **基础框架搭建**
   - 实现 AiChatController 和基础 API
   - 配置 OpenAI 连接
   - 实现对话历史存储

3. **函数实现**
   - 实现 4 个核心函数
   - 配置函数注册中心
   - 编写 System Prompt

4. **确认机制**
   - 实现 PendingAction 状态管理
   - 实现确认/取消流程

5. **前端开发**
   - 创建 AI 助手页面
   - 实现聊天界面组件
   - 集成确认弹窗

6. **测试验证**
   - 单元测试
   - 集成测试
   - 用户体验测试

### 11.2 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M4</version>
</dependency>
```

---

## 12. 成功标准

- [ ] 用户可以通过自然语言创建项目
- [ ] 用户可以通过自然语言添加费用记录
- [ ] 所有操作都需要用户确认后执行
- [ ] 支持多轮对话和上下文理解
- [ ] AI 解析失败时有友好的提示
- [ ] 响应时间 < 5 秒（不含网络延迟）

---

## 13. 后续优化

- 支持语音输入
- 支持更多操作类型（删除、修改）
- 支持批量导入数据
- 支持智能推荐和提醒
- 支持 Function Calling 结果缓存
