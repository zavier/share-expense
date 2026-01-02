# AI函数优化方案 v2.0

**文档版本**: v2.0
**创建日期**: 2025-01-02
**更新日期**: 2025-01-02
**参考文档**: [Writing effective tools for agents—Anthropic Engineering](https://www.anthropic.com/engineering/writing-tools-for-agents)
**目标模块**: share-expense-ai/src/main/java/com/github/zavier/ai/function/
**优化目标**: 提升AI函数调用准确率、降低token消耗、增强用户体验

---

## 📚 核心设计原则（基于Anthropic最佳实践）

根据Anthropic的工程实践，有效的AI工具应该遵循以下原则：

### 原则1: 选择正确的工具（适度整合，而非过度拆分）

**关键洞察**:
- ❌ **错误做法**: 为每个API端点创建一个工具（如`list_users`, `get_user_by_id`, `create_event`）
- ✅ **正确做法**: 针对高影响力工作流创建整合工具（如`schedule_event`整合查找可用性和创建会议）

**应用于费用分摊系统**:
- ✅ 工具应反映人类解决任务的思维方式
- ✅ 减少AI的选择困难，降低错误率
- ✅ 减少中间输出对上下文的消耗

---

### 原则2: 返回有意义的上下文（优先自然语言标识符）

**关键洞察**:
- ❌ **错误做法**: 返回`uuid`, `thread_ts`, `channel_id`等技术标识符
- ✅ **正确做法**: 返回`name`, `project_name`, `member_name`等自然语言标识符

**数据支撑**:
> "We've found that merely resolving arbitrary alphanumeric UUIDs to more semantically meaningful and interpretable language (or even a 0-indexed ID scheme) significantly improves Claude's precision in retrieval tasks by reducing hallucinations."

**应用于费用分摊系统**:
- 使用项目名称而非UUID
- 使用成员姓名而非用户ID
- 提供自然语言的成功/错误消息

---

### 原则3: 优化Token效率（提供灵活的响应格式）

**关键洞察**:
- LLM agents有有限的上下文窗口
- 工具应返回高信号信息，避免无关数据
- 提供`response_format`参数让agent控制详细程度

**应用于费用分摊系统**:
```java
enum ResponseFormat {
    CONCISE,  // 精简模式：只返回核心信息，无ID和元数据
    DETAILED  // 详细模式：包含所有信息和ID（用于后续工具调用）
}
```

**效果对比**:
- 精简模式：72 tokens（节省66%）
- 详细模式：206 tokens（完整信息）

---

### 原则4: Prompt工程工具描述（像对新员工描述一样清晰）

**关键洞察**:
- ❌ **模糊描述**: "获取项目信息"
- ✅ **清晰描述**: "查询项目的费用结算情况，显示每个人应付或应收的金额。使用场景：用户说'查询周末聚餐的结算'"

**关键要素**:
1. **清晰说明工具用途**: 这个工具是做什么的
2. **使用场景示例**: 何时调用这个工具
3. **参数格式说明**: 期望的输入格式
4. **注意事项**: 特殊的约束或要求

---

### 原则5: 命名空间（统一前缀，减少混淆）

**关键洞察**:
- 当有数十个MCP服务器和数百个工具时，命名冲突会导致混淆
- 使用统一前缀帮助AI快速识别相关工具

**应用于费用分摊系统**:
- 原则：所有工具使用`expense_`前缀
- 示例：`expense_create_project`, `expense_add_expense`, `expense_get_settlement`

---

## 一、当前问题分析（基于Anthropic原则）

### 1.1 工具设计问题

#### 问题1.1.1: 过度拆分的查询工具

**现状**:
- `GetProjectDetailsFunction`: 获取项目详情（名称、描述、成员）
- `ListProjectsFunction`: 列出所有项目（ID、名称、描述）
- `GetExpenseDetailsFunction`: 获取费用明细（264行代码，超长返回）

**问题**（根据原则1）:
- ❌ `ListProjectsFunction` 和 `GetProjectDetailsFunction` 功能重叠
- ❌ `GetExpenseDetailsFunction` 返回过多信息，浪费token
- ❌ AI需要判断何时调用哪个工具，增加选择困难

**Anthropic建议**:
> "Tools can consolidate functionality, handling potentially multiple discrete operations under the hood."

---

#### 问题1.1.2: 缺少响应格式控制

**现状**:
- 所有工具都返回固定格式的文本
- 无法根据场景控制返回内容的详细程度

**问题**（根据原则3）:
- ❌ 用户查看摘要时，仍接收完整的明细列表
- ❌ 浪费token，降低响应速度
- ❌ 增加上下文窗口压力

---

#### 问题1.1.3: 参数命名不够清晰

**现状**:
```java
addExpenseRecord(projectId, payer, amount, ...)
```

**问题**（根据原则4）:
- ❌ `projectId` 参数名暗示只接受ID，但用户更习惯使用名称
- ❌ 没有明确说明参数格式（如日期格式）
- ❌ 缺少使用场景说明

---

#### 问题1.1.4: 重复的工具方法

**现状**:
- `getExpenseDetails(projectId)` 和 `getExpenseDetailsByName(projectName)`
- `getSettlement(projectId)` 和 `getSettlementByName(projectName)`

**问题**（根据原则1）:
- ❌ 增加工具数量，浪费上下文
- ❌ AI需要判断该用哪个版本
- ❌ 违反"适度整合"原则

---

### 1.2 Token效率问题

#### 问题1.2.1: 返回信息过多

**现状**:
- `GetExpenseDetailsFunction` 返回：总览 + 按类型统计 + 按成员统计 + 明细列表
- 单次返回可能超过500 tokens

**问题**（根据原则3）:
- ❌ 用户可能只需要汇总统计
- ❌ 强制返回明细列表浪费token
- ❌ 限制单次对话可处理的信息量

---

#### 问题1.2.2: 缺少分页和截断机制

**现状**:
- `ListProjectsFunction` 一次返回50个项目
- `GetExpenseDetailsFunction` 返回所有费用记录

**问题**（根据原则3）:
- ❌ 项目很多时，返回内容过长
- ❌ 没有提供分页参数
- ❌ 可能超过token限制

---

### 1.3 工具描述问题

#### 问题1.3.1: 描述不够清晰

**现状**:
```java
@Tool(description = "添加一笔费用记录。需要提供项目ID、付款人、金额、费用类型、参与消费的成员列表。用于在用户要记录费用时，进行持久化保存")
```

**问题**（根据原则4）:
- ❌ 没有使用场景示例
- ❌ 参数格式说明不明确（如日期格式）
- ❌ 缺少注意事项（如成员必须在项目中）

---

## 二、优化方案（基于Anthropic最佳实践）

### 2.1 工具整合与重新设计

#### 优化措施1: 智能项目标识符（统一参数格式）

**原则应用**: 原则2（返回有意义的上下文）+ 原则5（命名空间）

**设计方案**:

所有与项目相关的工具统一使用`project_identifier`参数，支持：
- 项目名称（自然语言，如"周末聚餐"）
- 项目ID（数字，如"5"）
- 自动识别并解析

```java
/**
 * 智能解析项目标识符
 * @return 项目ID，未找到返回null
 */
private Integer resolveProjectIdentifier(String identifier) {
    // 1. 尝试解析为数字ID
    if (identifier.matches("\\d+")) {
        return Integer.parseInt(identifier);
    }

    // 2. 作为项目名称查找
    ProjectListQry qry = new ProjectListQry();
    qry.setName(identifier);
    qry.setPage(1);
    qry.setSize(10);

    PageResponse<ProjectDTO> response = projectService.pageProject(qry);
    if (response.getData().isEmpty()) {
        return null;
    }

    // 3. 精确匹配优先
    for (ProjectDTO project : response.getData()) {
        if (project.getProjectName().equals(identifier)) {
            return project.getProjectId();
        }
    }

    // 4. 模糊匹配（包含）
    for (ProjectDTO project : response.getData()) {
        if (project.getProjectName().contains(identifier)) {
            return project.getProjectId();
        }
    }

    // 5. 返回第一个结果
    return response.getData().get(0).getProjectId();
}
```

**好处**:
- ✅ 用户可以直接说"周末聚餐"，无需先查询ID
- ✅ 减少AI的工具调用次数
- ✅ 提升用户体验

---

#### 优化措施2: 添加响应格式控制（Token效率优化）

**原则应用**: 原则3（优化Token效率）

**设计方案**:

定义响应格式枚举：

```java
public enum ExpenseResponseFormat {
    /**
     * 精简模式：只返回核心信息，适用于用户查看摘要
     * - 不包含技术ID（project_id, member_id等）
     * - 不包含元数据（创建时间、更新时间等）
     * - 返回自然语言描述
     */
    CONCISE,

    /**
     * 详细模式：包含完整信息，适用于需要进一步处理的场景
     * - 包含所有ID和技术字段
     * - 包含元数据
     * - 返回结构化数据
     */
    DETAILED
}
```

**应用于 GetSettlementFunction**:

```java
@Tool(description = """
查询项目的费用结算情况，显示每个人应付或应收的金额。

参数说明：
- project_identifier: 项目名称或项目ID（如"周末聚餐"或"5"），自动识别
- response_format: 返回格式，可选值：
  * "concise": 精简模式（默认），只返回结算金额和自然语言说明
  * "detailed": 详细模式，包含所有字段和ID，用于后续处理

使用场景：
- 用户说"查询周末聚餐的结算"、"看看谁该给谁钱"
- AI需要获取项目ID进行后续操作时使用detailed模式

注意事项：
- 正数表示应收（别人欠他钱）
- 负数表示应付（他欠别人钱）
- 0表示已结清
""")
public String getSettlement(
        @ToolParam(description = "项目名称或项目ID") String projectIdentifier,
        @ToolParam(description = "返回格式：concise（精简）或detailed（详细）", required = false) String responseFormat) {

    Integer projectId = resolveProjectIdentifier(projectIdentifier);
    if (projectId == null) {
        return buildErrorResponse("未找到项目", projectIdentifier);
    }

    ExpenseResponseFormat format = parseResponseFormat(responseFormat);
    List<UserSharingDTO> settlements = fetchSettlements(projectId);

    if (format == ExpenseResponseFormat.CONCISE) {
        return buildConciseSettlement(projectIdentifier, settlements);
    } else {
        return buildDetailedSettlement(projectId, settlements);
    }
}

/**
 * 构建精简响应（约50-80 tokens）
 */
private String buildConciseSettlement(String projectName, List<UserSharingDTO> settlements) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("# %s 的结算情况\n\n", projectName));

    for (UserSharingDTO settlement : settlements) {
        BigDecimal amount = settlement.getPaidAmount().subtract(settlement.getConsumeAmount());

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("• %s：应收 %.2f 元\n", settlement.getMember(), amount));
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            sb.append(String.format("• %s：应付 %.2f 元\n", settlement.getMember(), amount.abs()));
        } else {
            sb.append(String.format("• %s：已结清\n", settlement.getMember()));
        }
    }

    return sb.toString();
}

/**
 * 构建详细响应（约150-200 tokens，包含ID）
 */
private String buildDetailedSettlement(Integer projectId, List<UserSharingDTO> settlements) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("# 项目 %d 结算详情\n\n", projectId));

    for (UserSharingDTO settlement : settlements) {
        BigDecimal amount = settlement.getPaidAmount().subtract(settlement.getConsumeAmount());

        sb.append(String.format("## %s（ID: %d）\n",
                settlement.getMember(), settlement.getMemberId()));
        sb.append(String.format("- 已付：%.2f 元\n", settlement.getPaidAmount()));
        sb.append(String.format("- 消费：%.2f 元\n", settlement.getConsumeAmount()));
        sb.append(String.format("- 结算：%.2f 元\n", amount));
        sb.append("\n");
    }

    return sb.toString();
}
```

**Token节省对比**:
- 精简模式（3人）：约60 tokens
- 详细模式（3人）：约180 tokens
- **节省：66%**

---

#### 优化措施3: 重新设计GetExpenseDetailsFunction（保持整合，添加格式控制）

**原则应用**: 原则1（适度整合）+ 原则3（Token效率）

**关键调整**：

❌ **原方案（v1.0）**: 拆分为两个函数
- `getExpenseSummary()`: 返回汇总统计
- `listExpenseRecords()`: 返回明细列表

✅ **新方案（v2.0）**: 保持整合，添加格式控制
- `getExpenseDetails(projectIdentifier, responseFormat, section)`

**理由**（根据Anthropic原则）:
> "Tools can consolidate functionality, handling potentially multiple discrete operations under the hood."

**设计方案**:

```java
public enum ExpenseDetailSection {
    /** 只返回汇总统计 */
    SUMMARY,
    /** 只返回明细列表 */
    RECORDS,
    /** 返回全部（汇总+明细） */
    ALL
}

@Tool(description = """
查询项目的费用信息，包括汇总统计和/或明细记录。

参数说明：
- project_identifier: 项目名称或项目ID（如"周末聚餐"或"5"）
- section: 返回内容，可选值：
  * "summary": 汇总统计（总览、按类型、按成员）
  * "records": 明细记录列表
  * "all": 全部内容（默认）
- response_format: 返回格式，可选值：
  * "concise": 精简模式，只返回核心信息
  * "detailed": 详细模式，包含所有字段和ID
- page_size: 明细记录数量限制，仅section="records"或"all"时有效，默认20，最大100

使用场景：
- 用户说"统计周末聚餐的总支出" → section="summary"
- 用户说"查看周末聚餐的所有消费记录" → section="records"
- 用户说"查看周末聚餐的完整费用信息" → section="all"

注意事项：
- summary模式通常返回50-80 tokens
- records模式根据page_size返回50-200 tokens
- 建议优先使用summary模式获取概况
""")
public String getExpenseDetails(
        @ToolParam(description = "项目名称或项目ID") String projectIdentifier,
        @ToolParam(description = "返回内容：summary/records/all", required = false) String section,
        @ToolParam(description = "返回格式：concise/detailed", required = false) String responseFormat,
        @ToolParam(description = "明细记录数量限制，默认20，最大100", required = false) Integer pageSize) {

    Integer projectId = resolveProjectIdentifier(projectIdentifier);
    if (projectId == null) {
        return buildErrorResponse("未找到项目", projectIdentifier);
    }

    ExpenseDetailSection detailSection = parseSection(section);
    ExpenseResponseFormat format = parseResponseFormat(responseFormat);

    List<ExpenseRecordDTO> records = fetchExpenseRecords(projectId);

    StringBuilder sb = new StringBuilder();

    // 根据section参数决定返回内容
    if (detailSection == ExpenseDetailSection.SUMMARY || detailSection == ExpenseDetailSection.ALL) {
        sb.append(buildExpenseSummary(projectIdentifier, records, format));
    }

    if (detailSection == ExpenseDetailSection.RECORDS || detailSection == ExpenseDetailSection.ALL) {
        if (detailSection == ExpenseDetailSection.ALL && !sb.isEmpty()) {
            sb.append("\n---\n\n");
        }
        sb.append(buildExpenseRecords(records, format, pageSize));
    }

    return sb.toString();
}
```

**好处**:
- ✅ 保持工具整合，减少AI选择困难
- ✅ 灵活控制返回内容，优化token使用
- ✅ 单个工具满足多种场景需求

---

#### 优化措施4: 优化ListProjectsFunction（添加格式控制）

**原则应用**: 原则3（Token效率）+ 原则4（清晰描述）

**设计方案**:

```java
@Tool(description = """
查询用户的所有费用分摊项目。

参数说明：
- name: 项目名称过滤（可选），支持模糊搜索
- include_members: 是否包含成员列表（默认false）
- response_format: 返回格式，可选值：
  * "concise": 精简模式（默认），只返回项目名称和描述
  * "detailed": 详细模式，包含项目ID和成员列表
- page_size: 返回项目数量限制，默认20，最大50

使用场景：
- 用户说"查看我的所有项目" → 精简模式
- 用户说"查看周末聚餐的成员" → 名称过滤 + 包含成员

注意事项：
- 默认返回最近的项目（按创建时间倒序）
- 建议优先使用concise模式减少token消耗
""")
public String listProjects(
        @ToolParam(description = "项目名称过滤（可选），支持模糊搜索", required = false) String name,
        @ToolParam(description = "是否包含成员列表", required = false) Boolean includeMembers,
        @ToolParam(description = "返回格式：concise/detailed", required = false) String responseFormat,
        @ToolParam(description = "返回项目数量限制，默认20，最大50", required = false) Integer pageSize) {

    // 实现逻辑...
}
```

---

#### 优化措施5: 优化AddExpenseRecordFunction（增强参数描述和验证）

**原则应用**: 原则4（Prompt工程）

**设计方案**:

```java
@Tool(description = """
添加一笔费用记录到指定项目。

参数说明：
- project_identifier: 项目名称或项目ID（如"周末聚餐"或"5"）
- payer: 付款人姓名，必须是项目成员
- amount: 金额，数字类型，单位元（如100.50）
- expense_type: 费用类型，如"餐饮"、"交通"、"住宿"、"娱乐"等
- consumers: 参与消费的成员列表，必须是项目成员，至少1人
- pay_date: 消费日期（可选），格式yyyy-MM-dd（如2024-01-15），不填默认今天
- remark: 备注说明（可选），记录消费的具体内容

使用场景：
- 用户说"记录一笔支出，Alice付了50元吃饭"
- 用户说"添加交通费，Bob花了20元地铁"

注意事项：
- 付款人和所有消费成员必须在项目成员列表中
- 金额必须大于0
- 日期必须为yyyy-MM-dd格式或为空

错误处理：
- 如果项目不存在，会返回明确的错误提示
- 如果成员不在项目中，会列出当前项目成员
- 如果参数格式错误，会返回具体的格式说明
""")
public String addExpenseRecord(
        @ToolParam(description = "项目名称或项目ID") String projectIdentifier,
        @ToolParam(description = "付款人姓名，必须是项目成员") String payer,
        @ToolParam(description = "金额，数字类型，单位元，必须大于0") BigDecimal amount,
        @ToolParam(description = "费用类型，如餐饮、交通、住宿、娱乐等") String expenseType,
        @ToolParam(description = "参与消费的成员列表，必须是项目成员，至少1人") List<String> consumers,
        @ToolParam(description = "消费日期，格式yyyy-MM-dd，不填默认今天", required = false) String payDate,
        @ToolParam(description = "备注说明（可选）", required = false) String remark) {

    // 1. 解析项目标识符
    Integer projectId = resolveProjectIdentifier(projectIdentifier);
    if (projectId == null) {
        return buildProjectNotFoundResponse(projectIdentifier);
    }

    // 2. 获取项目成员列表（用于验证）
    List<String> projectMembers = getProjectMembers(projectId);

    // 3. 验证付款人
    if (!projectMembers.contains(payer)) {
        return buildMemberNotFoundResponse("付款人", payer, projectMembers);
    }

    // 4. 验证消费成员
    List<String> invalidMembers = consumers.stream()
            .filter(member -> !projectMembers.contains(member))
            .collect(Collectors.toList());
    if (!invalidMembers.isEmpty()) {
        return buildMemberNotFoundResponse("消费成员", invalidMembers.get(0), projectMembers);
    }

    // 5. 验证金额
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        return "❌ 金额必须大于0，请检查输入";
    }

    // 6. 解析日期
    LocalDate date = parseDate(payDate);
    if (date == null) {
        return "❌ 日期格式错误，正确格式为：yyyy-MM-dd（如 2024-01-15）";
    }

    // 7. 调用业务逻辑
    // ...
}

/**
 * 构建项目未找到的错误响应
 */
private String buildProjectNotFoundResponse(String identifier) {
    return String.format("""
❌ 未找到项目"%s"

建议：
1. 使用 listProjects 查看所有项目
2. 检查项目名称是否正确
3. 可以使用项目ID（数字）代替项目名称
""", identifier);
}

/**
 * 构建成员未找到的错误响应
 */
private String buildMemberNotFoundResponse(String role, String memberName, List<String> validMembers) {
    return String.format("""
❌ %s"%s"不在项目成员列表中

当前项目成员：%s

建议：
1. 检查成员姓名是否正确
2. 使用 addMembers 添加新成员到项目
""", role, memberName, String.join("、", validMembers));
}
```

---

### 2.2 工具命名优化（统一前缀）

**原则应用**: 原则5（命名空间）

**设计方案**:

所有工具使用`expense_`前缀：

| 原函数名 | 新函数名 | 说明 |
|---------|---------|------|
| `listProjects` | `expense_list_projects` | 列出项目 |
| `createProject` | `expense_create_project` | 创建项目 |
| `addMembers` | `expense_add_members` | 添加成员 |
| `addExpenseRecord` | `expense_add_expense` | 添加费用 |
| `getProjectDetails` | **移除** | 合并到expense_list_projects |
| `getSettlement` | `expense_get_settlement` | 获取结算 |
| `getExpenseDetails` | `expense_get_expense_details` | 获取费用信息 |

**好处**:
- ✅ 快速识别相关工具
- ✅ 避免命名冲突
- ✅ 符合Anthropic建议的命名规范

---

### 2.3 工具整合优化

**整合决策对比表**:

| 工具 | v1.0方案 | v2.0方案 | 理由 |
|------|---------|---------|------|
| 费用查询 | 拆分为`getExpenseSummary`和`listExpenseRecords` | 保持`expense_get_expense_details`，添加`section`参数 | 原则1：适度整合，减少选择困难 |
| 项目查询 | `listProjects`和`getProjectDetails`分离 | 合并到`expense_list_projects`，添加`include_members`参数 | 原则1：一个工具满足多种场景 |
| 结算查询 | 移除重复的ById/ByName方法 | 统一为`expense_get_settlement`，智能识别参数 | 原则1：减少冗余，简化选择 |

---

## 三、最终工具列表（v2.0）

| 序号 | 工具名称 | 职责 | 主要参数 | 默认响应格式 |
|------|---------|------|----------|------------|
| 1 | `expense_list_projects` | 列出项目 | name, include_members, response_format, page_size | concise |
| 2 | `expense_create_project` | 创建项目 | project_name, description, members | - |
| 3 | `expense_add_members` | 添加成员 | project_identifier, members | - |
| 4 | `expense_add_expense` | 添加费用 | project_identifier, payer, amount, expense_type, consumers, pay_date, remark | - |
| 5 | `expense_get_settlement` | 获取结算 | project_identifier, response_format | concise |
| 6 | `expense_get_expense_details` | 获取费用信息 | project_identifier, section, response_format, page_size | summary, concise |

**变更说明**:
- ✅ 统一使用`expense_`前缀
- ✅ 统一使用`project_identifier`参数（名称或ID自动识别）
- ✅ 所有查询工具支持`response_format`参数（concise/detailed）
- ✅ 移除重复的ById/ByName方法
- ✅ 合并功能重叠的工具
- ✅ 添加分页和截断支持

---

## 四、Token效率对比

### 4.1 典型场景Token消耗

**场景1: 查询项目结算**

| 方案 | 工具调用 | Token消耗 | 说明 |
|------|---------|---------|------|
| v1.0 | `getSettlementById(5)` | ~200 tokens | 返回固定格式 |
| v2.0 | `expense_get_settlement("周末聚餐", "concise")` | ~60 tokens | 精简模式 |
| **节省** | - | **70%** | - |

**场景2: 查询费用汇总**

| 方案 | 工具调用 | Token消耗 | 说明 |
|------|---------|---------|------|
| v1.0 | `getExpenseDetailsByName("周末聚餐")` | ~500 tokens | 返回全部内容 |
| v2.0 | `expense_get_expense_details("周末聚餐", "summary", "concise")` | ~80 tokens | 只返回汇总 |
| **节省** | - | **84%** | - |

**场景3: 列出所有项目**

| 方案 | 工具调用 | Token消耗 | 说明 |
|------|---------|---------|------|
| v1.0 | `listProjects()` | ~300 tokens | 返回20个项目 |
| v2.0 | `expense_list_projects(null, false, "concise", 20)` | ~150 tokens | 精简模式 |
| **节省** | - | **50%** | - |

---

### 4.2 预期Token节省

假设典型对话包含以下工具调用：
1. 列出项目：1次
2. 查询结算：2次
3. 查询费用汇总：1次
4. 添加费用：1次

**v1.0 总Token消耗**: 200 + 500 + 400 + 150 = **1250 tokens**
**v2.0 总Token消耗**: 150 + 120 + 80 + 150 = **500 tokens**
**节省**: **60%**

---

## 五、实施计划

### 5.1 实施阶段

#### 阶段1: 基础设施准备（0.5天）

**任务清单**:
- [ ] 创建`ExpenseResponseFormat`枚举类
- [ ] 创建`ExpenseDetailSection`枚举类
- [ ] 创建`ProjectIdentifierResolver`工具类
- [ ] 创建`BaseExpenseFunction`基类
- [ ] 编写单元测试

---

#### 阶段2: 工具重构（2-3天）

**任务清单**:
- [ ] 重构`expense_get_settlement`（支持response_format）
- [ ] 重构`expense_get_expense_details`（添加section和response_format）
- [ ] 重构`expense_list_projects`（添加response_format和include_members）
- [ ] 重构`expense_add_expense`（增强描述和验证）
- [ ] 重构`expense_add_members`（支持project_identifier）
- [ ] 删除重复的工具方法
- [ ] 更新工具名称（添加expense_前缀）

---

#### 阶段3: 测试验证（1天）

**任务清单**:
- [ ] 运行所有单元测试
- [ ] 运行集成测试
- [ ] 手动测试典型场景（见下文）
- [ ] Token消耗对比测试

**典型测试场景**:

1. **基础查询场景**
   ```
   用户：查看我的所有项目
   AI：调用 expense_list_projects(null, false, "concise", 20)
   验证：返回精简的项目列表，包含项目名称和描述
   ```

2. **结算查询场景**
   ```
   用户：周末聚餐的结算情况怎么样？
   AI：调用 expense_get_settlement("周末聚餐", "concise")
   验证：返回精简的结算信息，自然语言描述
   ```

3. **费用汇总场景**
   ```
   用户：统计一下周末聚餐的总支出
   AI：调用 expense_get_expense_details("周末聚餐", "summary", "concise", null)
   验证：返回汇总统计，不包含明细列表
   ```

4. **费用明细场景**
   ```
   用户：查看周末聚餐的所有消费记录
   AI：调用 expense_get_expense_details("周末聚餐", "records", "concise", 20)
   验证：返回明细列表，限制20条
   ```

5. **添加费用场景**
   ```
   用户：记录一笔支出，Alice付了50元吃饭，我们3个人AA
   AI：调用 expense_add_expense("周末聚餐", "Alice", 50, "餐饮", ["Alice", "Bob", "Charlie"], null, null)
   验证：成功添加，返回明确的成功消息
   ```

6. **错误处理场景**
   ```
   用户：记录一笔支出，Alice付了50元
   AI：发现缺少consumers参数，询问用户
   验证：AI能够识别缺失参数并主动询问
   ```

---

#### 阶段4: 评估与优化（1天）

**任务清单**:
- [ ] 构建评估任务集（基于真实场景）
- [ ] 运行评估并收集指标
  - 工具调用准确率
  - Token消耗
  - 响应时间
  - 错误率
- [ ] 分析失败案例
- [ ] 迭代优化工具描述和实现

**评估任务示例**:

| 任务ID | 任务描述 | 预期工具调用 | 验证标准 |
|--------|---------|------------|---------|
| 1 | 查看"周末聚餐"项目的结算情况 | `expense_get_settlement("周末聚餐", "concise")` | 返回正确的结算金额 |
| 2 | 统计"周末聚餐"按类型的支出 | `expense_get_expense_details("周末聚餐", "summary", "concise", null)` | 返回按类型统计 |
| 3 | 列出所有包含"聚餐"的项目 | `expense_list_projects("聚餐", false, "concise", 20)` | 返回过滤后的项目列表 |
| 4 | 为"周末聚餐"添加一笔餐饮费用 | `expense_add_expense(...)` | 成功添加，返回确认消息 |
| 5 | 查看"周末聚餐"的最近10笔消费 | `expense_get_expense_details("周末聚餐", "records", "concise", 10)` | 返回10条记录 |

---

#### 阶段5: 文档更新（0.5天）

**任务清单**:
- [ ] 更新`CLAUDE.md`中的AI工具文档
- [ ] 编写工具使用示例
- [ ] 更新API文档
- [ ] 编写迁移指南（如有breaking changes）

---

### 5.2 时间安排

| 阶段 | 预计工时 | 开始日期 | 结束日期 |
|------|---------|---------|---------|
| 阶段1: 基础设施准备 | 0.5天 | 待定 | 待定 |
| 阶段2: 工具重构 | 2-3天 | 待定 | 待定 |
| 阶段3: 测试验证 | 1天 | 待定 | 待定 |
| 阶段4: 评估与优化 | 1天 | 待定 | 待定 |
| 阶段5: 文档更新 | 0.5天 | 待定 | 待定 |
| **总计** | **5-6天** | | |

---

## 六、预期效果

### 6.1 定量效果

| 指标 | v1.0 | v2.0 | 改善幅度 |
|------|------|------|----------|
| 工具总数 | 7个 | 6个 | -14% |
| 重复工具数 | 2对 | 0对 | -100% |
| 平均工具描述长度 | 80字 | 120字 | +50%（但更清晰） |
| Token消耗（每次对话） | ~1250 tokens | ~500 tokens | **-60%** |
| Token消耗（结算查询） | ~200 tokens | ~60 tokens | **-70%** |
| 代码重复率 | ~15% | <5% | -67% |
| AI调用准确率 | ~85% | >95% | +12% |

### 6.2 定性效果

- ✅ **工具整合度提升**: 减少工具数量，降低AI选择困难
- ✅ **Token效率显著提升**: 平均节省60%的token消耗
- ✅ **用户体验提升**: 支持自然语言参数（项目名称）
- ✅ **灵活性提升**: 通过response_format和section参数控制返回内容
- ✅ **错误处理改善**: 提供明确、可操作的错误提示
- ✅ **可维护性提升**: 减少代码重复，统一命名规范

---

## 七、风险评估与缓解

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 引入新的Bug | 中 | 高 | 完善单元测试和集成测试 |
| AI调用准确率下降 | 低 | 高 | 充分测试典型场景，必要时调整工具描述 |
| 向后兼容性问题 | 低 | 中 | 保留旧工具一段时间，标记为@Deprecated |
| 性能下降（智能识别） | 低 | 中 | 项目标识符解析增加缓存 |
| Token节省不如预期 | 低 | 低 | 通过评估验证实际效果 |

---

## 八、关键设计决策对比

### 8.1 v1.0 vs v2.0

| 决策点 | v1.0方案 | v2.0方案 | 依据（Anthropic原则） |
|--------|---------|---------|----------------------|
| GetExpenseDetails处理 | 拆分为两个函数 | 保持整合，添加section参数 | 原则1：适度整合，减少选择困难 |
| GetProjectDetails处理 | 保持独立 | 合并到listProjects | 原则1：工具应整合相关操作 |
| 响应格式 | 固定格式 | 可配置（concise/detailed） | 原则3：优化token效率 |
| 参数类型 | 只支持ID | 支持名称或ID自动识别 | 原则2：返回有意义的上下文 |
| 工具命名 | 无前缀 | 统一expense_前缀 | 原则5：命名空间 |
| 工具描述 | 简短描述 | 详细说明+场景+注意事项 | 原则4：Prompt工程 |
| 错误处理 | 简单错误消息 | 详细错误提示+建议 | 原则4：清晰描述 |

### 8.2 设计原则符合度检查

| Anthropic原则 | v2.0方案符合度 | 具体体现 |
|---------------|---------------|---------|
| 原则1: 选择正确的工具（适度整合） | ✅ 高 | 保持GetExpenseDetails整合，添加参数控制 |
| 原则2: 返回有意义的上下文 | ✅ 高 | 使用项目名称，支持自然语言标识符 |
| 原则3: 优化Token效率 | ✅ 高 | response_format参数，节省60%+ token |
| 原则4: Prompt工程 | ✅ 高 | 详细描述，包含使用场景和注意事项 |
| 原则5: 命名空间 | ✅ 高 | 统一expense_前缀 |

---

## 九、后续优化方向

### 9.1 短期优化（1-2周）

1. **增加更多实用工具**
   - `expense_update_expense`: 修改费用记录
   - `expense_delete_expense`: 删除费用记录
   - `expense_search_expenses`: 按条件搜索费用

2. **智能建议系统**
   - 基于用户意图自动推荐合适的工具
   - 根据上下文自动填充参数

---

### 9.2 长期优化（1-3个月）

1. **多轮对话优化**
   - 支持上下文记忆
   - 减少重复参数输入

2. **性能优化**
   - 项目标识符解析增加LRU缓存
   - 减少数据库查询次数

3. **评估体系完善**
   - 建立自动化评估pipeline
   - 持续监控工具调用准确率和token效率

---

## 十、附录

### 10.1 工具对照表（v1.0 → v2.0）

| v1.0工具 | v2.0工具 | 变更说明 |
|---------|---------|----------|
| `listProjects(name)` | `expense_list_projects(name, include_members, response_format, page_size)` | 添加响应格式控制和分页 |
| `createProject(...)` | `expense_create_project(...)` | 重命名，添加前缀 |
| `addMembers(projectId, members)` | `expense_add_members(project_identifier, members)` | 支持名称或ID |
| `addExpenseRecord(projectId, ...)` | `expense_add_expense(project_identifier, ...)` | 重命名，支持名称或ID，增强描述 |
| `getProjectDetails(projectId)` | **合并到** `expense_list_projects` | 功能重叠，合并 |
| `getSettlement(projectId)` | **移除** | 统一到下方 |
| `getSettlementByName(projectName)` | `expense_get_settlement(project_identifier, response_format)` | 重命名，添加格式控制 |
| `getExpenseDetails(projectId)` | **移除** | 统一到下方 |
| `getExpenseDetailsByName(projectName)` | `expense_get_expense_details(project_identifier, section, response_format, page_size)` | 重命名，添加内容控制 |

---

### 10.2 响应格式示例对比

**concise模式示例**:
```
# 周末聚餐 的结算情况

• 张三：应收 100.00 元
• 李四：应付 50.00 元
• 王五：应付 50.00 元
```

**detailed模式示例**:
```
# 项目 5 结算详情

## 张三（ID: 101）
- 已付：200.00 元
- 消费：100.00 元
- 结算：100.00 元

## 李四（ID: 102）
- 已付：50.00 元
- 消费：100.00 元
- 结算：-50.00 元

## 王五（ID: 103）
- 已付：50.00 元
- 消费：100.00 元
- 结算：-50.00 元
```

---

### 10.3 代码文件变更清单

**新增文件**:
- `ExpenseResponseFormat.java`
- `ExpenseDetailSection.java`
- `ProjectIdentifierResolver.java`
- `BaseExpenseFunction.java`

**修改文件**:
- `ListProjectsFunction.java` → 重命名为 `ExpenseListProjectsFunction.java`
- `CreateProjectFunction.java` → 重命名为 `ExpenseCreateProjectFunction.java`
- `AddMembersFunction.java` → 重命名为 `ExpenseAddMembersFunction.java`
- `AddExpenseRecordFunction.java` → 重命名为 `ExpenseAddExpenseFunction.java`
- `GetSettlementFunction.java` → 重命名为 `ExpenseGetSettlementFunction.java`
- `GetExpenseDetailsFunction.java` → 重命名为 `ExpenseGetExpenseDetailsFunction.java`

**删除文件**:
- `GetProjectDetailsFunction.java`（功能合并到ExpenseListProjectsFunction）

---

## 十一、总结

本优化方案基于Anthropic的《Writing effective tools for agents》最佳实践，对AI函数工具进行了全面重新设计：

**核心改进**:
1. ✅ **适度整合**：保持工具整合，减少选择困难（原则1）
2. ✅ **自然语言优先**：支持项目名称，自动识别ID（原则2）
3. ✅ **Token效率优化**：response_format参数，节省60%+ token（原则3）
4. ✅ **清晰的描述**：详细的工具说明和使用场景（原则4）
5. ✅ **统一命名**：expense_前缀，清晰命名空间（原则5）

**预期效果**:
- Token消耗降低60%
- AI调用准确率提升到95%+
- 代码重复率降低到<5%
- 用户体验显著提升

**实施周期**: 5-6天

---

**文档结束**
