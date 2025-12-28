# AI 助手项目详情查询功能设计

## 背景

当前 AI 助手在用户添加费用记录时，如果用户说"剩余人员平分"，AI 不知道项目中有哪些成员，需要额外询问用户确认成员列表。这增加了交互轮次，用户体验不佳。

## 方案

新增一个查询项目详情的工具函数，让 AI 在需要时主动获取项目的基本信息和成员列表。

## 设计

### 函数签名

创建 `GetProjectDetailsFunction` 类，提供一个方法：

```java
@Tool(description = "根据项目ID查询项目的详细信息，包括项目名称、描述和成员列表")
public String getProjectDetails(@ToolParam(description = "项目ID") Integer projectId)
```

### 返回格式

纯文本格式，例如：

```
项目详情：
- 项目ID：5
- 项目名称：周末聚餐
- 项目描述：周末和朋友的聚餐费用
- 成员列表（5人）：张三、李四、王五、赵六、钱七
```

如果项目没有描述，描述字段省略：
```
项目详情：
- 项目ID：5
- 项目名称：周末聚餐
- 成员列表（5人）：张三、李四、王五、赵六、钱七
```

如果项目没有成员：
```
项目详情：
- 项目ID：5
- 项目名称：周末聚餐
- 成员列表：暂无成员
```

### System Prompt 更新

在 `AiChatServiceImpl` 的 `SYSTEM_PROMPT` 中添加提示：

```
**重要提示：**
- 查询结算时，优先使用项目名称（getSettlementByName），而不是项目ID
- 如果用户提到项目名称但工具需要项目ID，先调用 listProjects 查找项目
- 只有当用户明确知道项目ID时，才使用 getSettlement
- 当需要添加费用记录或添加成员时，如果不确定项目的成员信息，先调用 getProjectDetails 获取项目详情
```

### 实现方式

在 AI Function 层组合调用现有服务：
1. 调用 `ProjectService.pageProject()` 获取项目基本信息
2. 调用 `ProjectService.listProjectMember()` 获取成员列表
3. 组装成纯文本返回

### 涉及文件

1. **新建：** `share-expense-ai/src/main/java/com/github/zavier/ai/function/GetProjectDetailsFunction.java`
2. **修改：** `share-expense-ai/src/main/java/com/github/zavier/ai/impl/AiChatServiceImpl.java`
   - 注入 `GetProjectDetailsFunction`
   - 在 `defaultTools()` 中添加该函数
   - 更新 `SYSTEM_PROMPT`

## 使用场景

AI 会在以下场景主动调用 `getProjectDetails`：
- 用户说"添加费用，剩余人员平分"时
- 用户提到项目但不确定有哪些成员时
- 任何需要验证成员信息的场景
