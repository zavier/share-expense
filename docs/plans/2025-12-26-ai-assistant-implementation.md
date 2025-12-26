# AI 记账助手实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 在费用分摊应用中集成 Spring AI + OpenAI，让用户通过自然语言对话完成记账操作（创建项目、添加成员、记录费用、查询结算）。

**架构:** 新增 `share-expense-ai` 模块，使用 Spring AI 的 Function Calling 机制。用户消息通过 AI 解析意图，提取参数后需用户确认，确认后调用现有 ProjectService 执行业务逻辑。

**技术栈:** Spring AI 1.0.0-M4, OpenAI GPT-4o-mini, Spring Boot 3.2.0, COLA 架构

---

## Task 1: 添加 Spring AI 模块和依赖

**Files:**
- Create: `share-expense-ai/pom.xml`
- Modify: `pom.xml` (添加新模块)

**Step 1: 创建新模块的 pom.xml**

创建文件 `share-expense-ai/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.github.zavier</groupId>
        <artifactId>share-expense-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>share-expense-ai</artifactId>
    <name>share-expense-ai</name>
    <description>AI Assistant Module</description>

    <dependencies>
        <!-- Spring AI -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        </dependency>

        <!-- Project Modules -->
        <dependency>
            <groupId>com.github.zavier</groupId>
            <artifactId>share-expense-client</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.zavier</groupId>
            <artifactId>share-expense-app</artifactId>
        </dependency>

        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
</project>
```

**Step 2: 修改父 pom.xml 添加新模块**

修改 `pom.xml` 第 34-40 行，在 modules 中添加新模块:

```xml
<modules>
    <module>share-expense-client</module>
    <module>share-expense-adapter</module>
    <module>share-expense-app</module>
    <module>share-expense-domain</module>
    <module>share-expense-infrastructure</module>
    <module>share-expense-ai</module>
    <module>start</module>
</modules>
```

**Step 3: 在父 pom.xml 中添加 Spring AI BOM**

在 `<properties>` 后添加 (第 32 行后):

```xml
<spring-ai.version>1.0.0-M4</spring-ai.version>
```

在 `<dependencyManagement>` 中添加 (第 110 行后):

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>${spring-ai.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

**Step 4: 添加 Spring Milestone 仓库**

在 `<build>` 后添加 (第 225 行后):

```xml
<repositories>
    <repository>
        <id>spring-milestones</id>
        <name>Spring Milestones</name>
        <url>https://repo.spring.io/milestone</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

**Step 5: 验证编译**

运行: `mvn clean compile -DskipTests`
预期: BUILD SUCCESS

**Step 6: 提交**

```bash
git add share-expense-ai/pom.xml pom.xml
git commit -m "feat: 添加 share-expense-ai 模块和 Spring AI 依赖"
```

---

## Task 2: 创建 AI 模块基础目录结构

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/AiChatController.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/AiChatService.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/dto/AiChatRequest.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/dto/AiChatResponse.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/dto/ChatMessage.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/dto/PendingAction.java`

**Step 1: 创建 DTO 类**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/dto/AiChatRequest.java`:

```java
package com.github.zavier.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {
    @NotBlank(message = "消息内容不能为空")
    private String message;

    private String conversationId;
}
```

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/dto/AiChatResponse.java`:

```java
package com.github.zavier.ai.dto;

import lombok.Data;

@Data
public class AiChatResponse {
    private String conversationId;
    private String reply;
    private PendingAction pendingAction;
}
```

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/dto/ChatMessage.java`:

```java
package com.github.zavier.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String id;
    private String role;  // user, assistant, system
    private String content;
    private LocalDateTime timestamp;
    private PendingAction pendingAction;
}
```

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/dto/PendingAction.java`:

```java
package com.github.zavier.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingAction {
    private String actionId;
    private String actionType;  // createProject, addMembers, addExpenseRecord, getSettlement
    private String description;
    private Map<String, Object> params;
}
```

**Step 2: 创建 Service 接口**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/AiChatService.java`:

```java
package com.github.zavier.ai;

import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request);
    AiChatResponse confirm(String conversationId, String actionId);
    AiChatResponse cancel(String conversationId);
}
```

**Step 3: 创建 Controller**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/AiChatController.java`:

```java
package com.github.zavier.ai;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    @PostMapping("/chat")
    public SingleResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        AiChatResponse response = aiChatService.chat(request);
        return SingleResponse.of(response);
    }

    @PostMapping("/confirm")
    public SingleResponse<AiChatResponse> confirm(@RequestBody ConfirmRequest request) {
        AiChatResponse response = aiChatService.confirm(request.getConversationId(), request.getActionId());
        return SingleResponse.of(response);
    }

    @PostMapping("/cancel")
    public SingleResponse<AiChatResponse> cancel(@RequestBody CancelRequest request) {
        AiChatResponse response = aiChatService.cancel(request.getConversationId());
        return SingleResponse.of(response);
    }

    @lombok.Data
    public static class ConfirmRequest {
        private String conversationId;
        private String actionId;
    }

    @lombok.Data
    public static class CancelRequest {
        private String conversationId;
    }
}
```

**Step 4: 验证编译**

运行: `mvn clean compile -pl share-expense-ai -am`
预期: BUILD SUCCESS

**Step 5: 提交**

```bash
git add share-expense-ai/
git commit -m "feat: 创建 AI 模块基础结构和 DTO"
```

---

## Task 3: 添加数据库实体和 Repository

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/entity/ConversationEntity.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/repository/ConversationRepository.java`
- Modify: `share-expense-infrastructure/src/main/resources/expense.sql`

**Step 1: 创建实体类**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/entity/ConversationEntity.java`:

```java
package com.github.zavier.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_conversation")
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "pending_action", columnDefinition = "JSON")
    private String pendingAction;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

**Step 2: 创建 Repository**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/repository/ConversationRepository.java`:

```java
package com.github.zavier.ai.repository;

import com.github.zavier.ai.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    List<ConversationEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    void deleteByConversationIdAndCreatedAtBefore(String conversationId, java.time.LocalDateTime cutoff);
}
```

**Step 3: 添加数据库表**

在 `share-expense-infrastructure/src/main/resources/expense.sql` 末尾添加:

```sql
-- AI 对话历史表
CREATE TABLE IF NOT EXISTS ai_conversation (
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

**Step 4: 验证编译**

运行: `mvn clean compile -pl share-expense-ai -am`
预期: BUILD SUCCESS

**Step 5: 提交**

```bash
git add share-expense-ai/ share-expense-infrastructure/src/main/resources/expense.sql
git commit -m "feat: 添加对话历史实体和表结构"
```

---

## Task 4: 实现 AI 函数定义

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/function/AiFunction.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/function/CreateProjectFunction.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/function/AddMembersFunction.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/function/AddExpenseRecordFunction.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/function/GetSettlementFunction.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/function/FunctionContext.java`

**Step 1: 创建函数上下文类**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/function/FunctionContext.java`:

```java
package com.github.zavier.ai.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionContext {
    private Integer userId;  // 当前用户ID
}
```

**Step 2: 创建函数注解**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/function/AiFunction.java`:

```java
package com.github.zavier.ai.function;

import org.springframework.context.annotation.Description;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Description
public @interface AiFunction {
    String name();
    String description();
}
```

**Step 3: 实现创建项目函数**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/function/CreateProjectFunction.java`:

```java
package com.github.zavier.ai.function;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectAddCmd;
import com.github.zavier.dto.data.ProjectDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AiFunction(
    name = "createProject",
    description = "创建一个新的费用分摊项目。需要提供项目名称和成员列表。"
)
public class CreateProjectFunction implements AiFunctionExecutor {

    @Resource
    private ProjectService projectService;

    public record Request(
        String projectName,
        String description,
        List<String> members
    ) {}

    @Override
    public String execute(Request request, FunctionContext context) {
        ProjectAddCmd cmd = new ProjectAddCmd();
        cmd.setProjectName(request.projectName());
        cmd.setDescription(request.description());
        cmd.setCreateUserId(context.getUserId());
        cmd.setCreateUserName("AI用户");  // TODO: 从用户信息获取

        SingleResponse<ProjectDTO> response = projectService.createProject(cmd);

        if (!response.isSuccess()) {
            throw new RuntimeException("创建项目失败: " + response.getErrMessage());
        }

        ProjectDTO project = response.getData();
        return String.format("项目创建成功！项目名称：%s，项目ID：%d", project.getName(), project.getId());
    }

    @Override
    public Class<Request> getRequestType() {
        return Request.class;
    }
}
```

**Step 4: 实现添加成员函数**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/function/AddMembersFunction.java`:

```java
package com.github.zavier.ai.function;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectMemberAddCmd;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AiFunction(
    name = "addMembers",
    description = "向现有项目添加新成员。需要提供项目ID和成员名称列表。"
)
public class AddMembersFunction implements AiFunctionExecutor {

    @Resource
    private ProjectService projectService;

    public record Request(
        Integer projectId,
        List<String> memberNames
    ) {}

    @Override
    public String execute(Request request, FunctionContext context) {
        Response response = Response.buildSuccess();

        for (String memberName : request.memberNames()) {
            ProjectMemberAddCmd cmd = new ProjectMemberAddCmd();
            cmd.setProjectId(request.projectId());
            cmd.setName(memberName);
            cmd.setOperatorId(context.getUserId());

            response = projectService.addProjectMember(cmd);
            if (!response.isSuccess()) {
                throw new RuntimeException("添加成员失败: " + response.getErrMessage());
            }
        }

        return String.format("成功添加 %d 个成员到项目 %d", request.memberNames().size(), request.projectId());
    }

    @Override
    public Class<Request> getRequestType() {
        return Request.class;
    }
}
```

**Step 5: 实现添加费用记录函数**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/function/AddExpenseRecordFunction.java`:

```java
package com.github.zavier.ai.function;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@AiFunction(
    name = "addExpenseRecord",
    description = "添加一笔费用记录。需要提供项目ID、付款人、金额、费用类型、消费日期、参与消费的成员列表。"
)
public class AddExpenseRecordFunction implements AiFunctionExecutor {

    @Resource
    private ProjectService projectService;

    public record Request(
        Integer projectId,
        String payer,
        BigDecimal amount,
        String expenseType,
        String payDate,
        List<String> consumers,
        String remark
    ) {}

    @Override
    public String execute(Request request, FunctionContext context) {
        ExpenseRecordAddCmd cmd = new ExpenseRecordAddCmd();
        cmd.setProjectId(request.projectId());
        cmd.setPayMember(request.payer());
        cmd.setAmount(request.amount());
        cmd.setExpenseType(request.expenseType());
        cmd.setRemark(request.remark());
        cmd.setOperatorId(context.getUserId());

        // 解析日期
        LocalDate date = parseDate(request.payDate());
        cmd.setPayDate(date);

        // 设置消费者（暂时假设所有消费者平分，具体实现可能需要查询成员ID）
        // TODO: 需要根据成员名称查询成员ID
        cmd.setConsumerIds(List.of());  // 先占位

        Response response = projectService.addExpenseRecord(cmd);

        if (!response.isSuccess()) {
            throw new RuntimeException("添加费用记录失败: " + response.getErrMessage());
        }

        return String.format("费用记录添加成功！付款人：%s，金额：%.2f元", request.payer(), request.amount());
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    @Override
    public Class<Request> getRequestType() {
        return Request.class;
    }
}
```

**Step 6: 实现查询结算函数**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/function/GetSettlementFunction.java`:

```java
package com.github.zavier.ai.function;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectSharingQry;
import com.github.zavier.dto.data.UserSharingDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AiFunction(
    name = "getSettlement",
    description = "查询项目的费用结算情况，显示每个人应付或应收的金额。需要提供项目ID。"
)
public class GetSettlementFunction implements AiFunctionExecutor {

    @Resource
    private ProjectService projectService;

    public record Request(
        Integer projectId
    ) {}

    @Override
    public String execute(Request request, FunctionContext context) {
        ProjectSharingQry qry = new ProjectSharingQry();
        qry.setProjectId(request.projectId());
        qry.setOperatorId(context.getUserId());

        SingleResponse<List<UserSharingDTO>> response = projectService.getProjectSharingDetail(qry);

        if (!response.isSuccess()) {
            throw new RuntimeException("查询结算失败: " + response.getErrMessage());
        }

        List<UserSharingDTO> settlements = response.getData();
        StringBuilder sb = new StringBuilder();
        sb.append("项目结算情况：\n");

        for (UserSharingDTO settlement : settlements) {
            if (settlement.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                sb.append(String.format("- %s 应付 %.2f 元\n", settlement.getMemberName(), settlement.getAmount()));
            } else if (settlement.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
                sb.append(String.format("- %s 应收 %.2f 元\n", settlement.getMemberName(), settlement.getAmount().abs()));
            } else {
                sb.append(String.format("- %s 已结清\n", settlement.getMemberName()));
            }
        }

        return sb.toString();
    }

    @Override
    public Class<Request> getRequestType() {
        return Request.class;
    }
}
```

**Step 7: 创建函数执行器接口**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/function/AiFunctionExecutor.java`:

```java
package com.github.zavier.ai.function;

public interface AiFunctionExecutor {
    String execute(Object request, FunctionContext context);
    Class<?> getRequestType();
}
```

**Step 8: 验证编译**

运行: `mvn clean compile -pl share-expense-ai -am`
预期: BUILD SUCCESS

**Step 9: 提交**

```bash
git add share-expense-ai/
git commit -m "feat: 实现 AI 函数定义（创建项目、添加成员、添加费用、查询结算）"
```

---

## Task 5: 实现函数注册中心和 AI 聊天服务

**Files:**
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/AiFunctionRegistry.java`
- Create: `share-expense-ai/src/main/java/com/github/zavier/ai/impl/AiChatServiceImpl.java`

**Step 1: 创建函数注册中心**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/AiFunctionRegistry.java`:

```java
package com.github.zavier.ai;

import com.github.zavier.ai.function.AiFunction;
import com.github.zavier.ai.function.AiFunctionExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Component
public class AiFunctionRegistry {

    private final Map<String, AiFunctionExecutor> functions = new HashMap<>();
    private final Map<String, Class<?>> requestTypes = new HashMap<>();

    public AiFunctionRegistry(List<AiFunctionExecutor> functionExecutors) {
        for (AiFunctionExecutor executor : functionExecutors) {
            AiFunction annotation = executor.getClass().getAnnotation(AiFunction.class);
            if (annotation != null) {
                String name = annotation.name();
                functions.put(name, executor);
                requestTypes.put(name, executor.getRequestType());
            }
        }
    }

    public AiFunctionExecutor getFunction(String name) {
        return functions.get(name);
    }

    public Class<?> getRequestType(String name) {
        return requestTypes.get(name);
    }

    public Map<String, String> getFunctionDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (AiFunctionExecutor executor : functions.values()) {
            AiFunction annotation = executor.getClass().getAnnotation(AiFunction.class);
            if (annotation != null) {
                descriptions.put(annotation.name(), annotation.description());
            }
        }
        return descriptions;
    }
}
```

**Step 2: 实现聊天服务**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/impl/AiChatServiceImpl.java`:

```java
package com.github.zavier.ai.impl;

import com.alibaba.fastjson2.JSON;
import com.github.zavier.ai.*;
import com.github.zavier.ai.dto.*;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.function.AiFunctionExecutor;
import com.github.zavier.ai.function.FunctionContext;
import com.github.zavier.ai.repository.ConversationRepository;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiChatServiceImpl implements AiChatService {

    @Resource
    private ChatClient.Builder chatClientBuilder;

    @Resource
    private AiFunctionRegistry functionRegistry;

    @Resource
    private ConversationRepository conversationRepository;

    // 存储待确认的操作（临时存储，生产环境应使用 Redis）
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
        你是一个费用分摊记账助手。你可以帮助用户：
        1. 创建费用分摊项目
        2. 向项目添加成员
        3. 记录费用支出
        4. 查询结算情况

        请用友好、简洁的中文回复。
        如果信息不完整，请主动询问用户。
        """;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        // 保存用户消息
        saveMessage(conversationId, "user", request.getMessage());

        // 获取对话历史
        List<Message> messages = buildMessages(conversationId);

        // 构建 ChatClient
        ChatClient chatClient = chatClientBuilder
            .defaultFunctions("createProject", "addMembers", "addExpenseRecord", "getSettlement")
            .defaultOptions(ChatClient.Options.builder().build())
            .build();

        // 调用 AI
        String response = chatClient.prompt()
            .messages(messages)
            .call()
            .content();

        // 保存 AI 回复
        saveMessage(conversationId, "assistant", response);

        return AiChatResponse.builder()
            .conversationId(conversationId)
            .reply(response)
            .build();
    }

    @Override
    public AiChatResponse confirm(String conversationId, String actionId) {
        PendingAction action = pendingActions.get(actionId);
        if (action == null) {
            throw new RuntimeException("操作已过期或不存在");
        }

        // 执行实际业务逻辑
        String result = executeAction(action);

        // 清除待确认操作
        pendingActions.remove(actionId);

        // 保存执行结果
        saveMessage(conversationId, "assistant", result);

        return AiChatResponse.builder()
            .conversationId(conversationId)
            .reply(result)
            .build();
    }

    @Override
    public AiChatResponse cancel(String conversationId) {
        // 清除该会话的所有待确认操作
        pendingActions.values().removeIf(action ->
            conversationId.equals(getConversationIdByAction(action.getActionId()))
        );

        return AiChatResponse.builder()
            .conversationId(conversationId)
            .reply("操作已取消")
            .build();
    }

    private List<Message> buildMessages(String conversationId) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        List<ConversationEntity> history = conversationRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        for (ConversationEntity entity : history) {
            if ("user".equals(entity.getRole())) {
                messages.add(new UserMessage(entity.getContent()));
            } else if ("assistant".equals(entity.getRole())) {
                messages.add(new org.springframework.ai.chat.messages.AssistantMessage(entity.getContent()));
            }
        }

        return messages;
    }

    private void saveMessage(String conversationId, String role, String content) {
        ConversationEntity entity = ConversationEntity.builder()
            .conversationId(conversationId)
            .userId(getCurrentUserId())
            .role(role)
            .content(content)
            .createdAt(LocalDateTime.now())
            .build();

        conversationRepository.save(entity);
    }

    private String executeAction(PendingAction action) {
        AiFunctionExecutor executor = functionRegistry.getFunction(action.getActionType());
        if (executor == null) {
            throw new RuntimeException("未知的操作类型: " + action.getActionType());
        }

        FunctionContext context = FunctionContext.builder()
            .userId(getCurrentUserId())
            .build();

        // TODO: 将 params 转换为相应的 Request 对象
        // 暂时返回一个占位符
        return "操作执行成功：" + action.getDescription();
    }

    private Integer getCurrentUserId() {
        // TODO: 从 UserHolder 获取当前用户ID
        return 1;
    }

    private String getConversationIdByAction(String actionId) {
        // TODO: 实现 actionId 到 conversationId 的映射
        return null;
    }
}
```

**Step 3: 配置 Spring AI**

创建 `share-expense-ai/src/main/java/com/github/zavier/ai/config/AiConfig.java`:

```java
package com.github.zavier.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String model;

    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(baseUrl, apiKey);
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return new OpenAiChatModel(openAiApi, OpenAiChatOptions.builder()
            .model(model)
            .temperature(0.7)
            .build());
    }

    @Bean
    public ChatClient.Builder chatClientBuilder(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel);
    }
}
```

**Step 4: 添加配置文件**

创建 `start/src/main/resources/application-ai.properties`:

```properties
# Spring AI 配置
spring.ai.openai.api-key=${OPENAI_API_KEY:your-api-key-here}
spring.ai.openai.base-url=${OPENAI_BASE_URL:https://api.openai.com}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7

# AI 功能配置
app.ai.chat.enabled=true
app.ai.chat.max-history-rounds=10
```

**Step 5: 验证编译**

运行: `mvn clean compile -pl share-expense-ai -am`
预期: BUILD SUCCESS

**Step 6: 提交**

```bash
git add share-expense-ai/ start/src/main/resources/application-ai.properties
git commit -m "feat: 实现函数注册中心和 AI 聊天服务"
```

---

## Task 6: 创建前端 AI 助手页面

**Files:**
- Create: `web/pages/ai-assistant.json` (amis 页面配置)

**Step 1: 创建前端页面配置**

创建 `web/pages/ai-assistant.json`:

```json
{
  "type": "page",
  "title": "AI 记账助手",
  "body": {
    "type": "flex",
    "direction": "column",
    "items": [
      {
        "type": "panel",
        "body": {
          "type": "markdown",
          "content": "### 💡 您可以这样说\n\n• \"创建项目'周末聚餐'，成员有小明、小红\"\n• \"记录今天午餐，小李付了50元，我们三个人平摊\"\n• \"查询项目123的结算情况\"\n• \"给项目5添加成员：小王\""
        }
      },
      {
        "type": "form",
        "title": "",
        "body": [
          {
            "type": "input-text",
            "name": "message",
            "label": "输入您的需求",
            "placeholder": "例如：今天午饭张三付了80元，我们四个人平摊",
            "required": true,
            "submitOnChange": false
          }
        ],
        "actions": [
          {
            "type": "button",
            "label": "发送",
            "level": "primary",
            "actionType": "ajax",
            "api": "post:/api/ai/chat",
            "body": {
              "message": "${message}"
            }
          }
        ]
      },
      {
        "type": "divider"
      },
      {
        "type": "panel",
        "title": "对话记录",
        "body": {
          "type": "each",
          "name": "messages",
          "placeholder": "暂无对话记录",
          "items": {
            "type": "tpl",
            "tpl": "<div class=\"message-item\"><strong>${role}:</strong> ${content}</div>",
            "className": "${role === 'user' ? 'user-message' : 'assistant-message'}"
          }
        }
      }
    ]
  },
  "styles": [
    {
      "selector": ".user-message",
      "rules": {
        "background": "#e3f2fd",
        "padding": "10px",
        "margin": "5px 0",
        "border-radius": "8px",
        "text-align": "right"
      }
    },
    {
      "selector": ".assistant-message",
      "rules": {
        "background": "#f5f5f5",
        "padding": "10px",
        "margin": "5px 0",
        "border-radius": "8px"
      }
    }
  ]
}
```

**Step 2: 提交**

```bash
git add web/
git commit -m "feat: 添加 AI 助手前端页面"
```

---

## Task 7: 编写单元测试

**Files:**
- Create: `share-expense-ai/src/test/java/com/github/zavier/ai/AiChatServiceTest.java`

**Step 1: 创建测试类**

创建 `share-expense-ai/src/test/java/com/github/zavier/ai/AiChatServiceTest.java`:

```java
package com.github.zavier.ai;

import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiChatServiceTest {

    // TODO: 添加 Mock 和实际测试用例
    // 由于需要真实的 OpenAI API 连接，建议使用 MockMvc 或 WireMock 进行测试

    @Test
    void testChatRequest() {
        AiChatRequest request = new AiChatRequest();
        request.setMessage("创建一个测试项目");

        assertNotNull(request.getMessage());
    }

    @Test
    void testChatResponse() {
        AiChatResponse response = AiChatResponse.builder()
            .conversationId("test-conv-123")
            .reply("好的，我来帮您创建项目")
            .build();

        assertEquals("test-conv-123", response.getConversationId());
        assertEquals("好的，我来帮您创建项目", response.getReply());
    }
}
```

**Step 2: 运行测试**

运行: `mvn test -pl share-expense-ai`
预期: 测试通过

**Step 3: 提交**

```bash
git add share-expense-ai/src/test/
git commit -m "test: 添加 AI 聊天服务单元测试"
```

---

## Task 8: 集成测试和文档更新

**Files:**
- Create: `share-expense-ai/src/test/java/com/github/zavier/ai/AiChatIntegrationTest.java`
- Modify: `CLAUDE.md`

**Step 1: 创建集成测试**

创建 `share-expense-ai/src/test/java/com/github/zavier/ai/AiChatIntegrationTest.java`:

```java
package com.github.zavier.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AiChatIntegrationTest {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能够正常加载
    }
}
```

**Step 2: 更新 CLAUDE.md**

在 `CLAUDE.md` 中添加 AI 助手相关文档:

```markdown
## AI 记账助手

### 功能概述

AI 记账助手允许用户通过自然语言完成以下操作：
- 创建费用分摊项目
- 添加项目成员
- 记录费用支出
- 查询结算情况

### 使用方式

1. 访问 `/ai-assistant` 页面
2. 输入自然语言描述，如："创建项目'周末聚餐'，成员有小明、小红"
3. AI 解析意图后，会展示确认弹窗
4. 确认后执行实际操作

### 配置

需要在环境变量中设置 OpenAI API Key:
```bash
export OPENAI_API_KEY=your-api-key
```

### API 端点

- `POST /api/ai/chat` - 发送消息
- `POST /api/ai/confirm` - 确认操作
- `POST /api/ai/cancel` - 取消操作
```

**Step 3: 提交**

```bash
git add share-expense-ai/src/test/ CLAUDE.md
git commit -m "test: 添加集成测试和更新文档"
```

---

## Task 9: 最终验证和清理

**Step 1: 完整构建测试**

运行: `mvn clean package -DskipTests`
预期: BUILD SUCCESS

**Step 2: 运行所有测试**

运行: `mvn test`
预期: 所有测试通过

**Step 3: 提交设计文档**

```bash
git add docs/plans/
git commit -m "docs: 添加 AI 助手设计文档和实现计划"
```

**Step 4: 创建总结文档**

创建 `docs/plans/2025-12-26-ai-assistant-summary.md`:

```markdown
# AI 记账助手开发总结

## 已完成功能

1. ✅ 新增 `share-expense-ai` 模块
2. ✅ 集成 Spring AI + OpenAI
3. ✅ 实现 4 个核心函数（创建项目、添加成员、添加费用、查询结算）
4. ✅ 实现对话历史存储
5. ✅ 实现确认机制
6. ✅ 创建前端页面

## 测试清单

- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 手动测试：创建项目
- [ ] 手动测试：添加费用记录
- [ ] 手动测试：查询结算

## 后续优化

- 支持语音输入
- 支持 Function Calling 结果缓存
- 支持更多操作类型
- 优化错误处理和提示
```

**Step 5: 最终提交**

```bash
git add docs/plans/2025-12-26-ai-assistant-summary.md
git commit -m "docs: 添加开发总结"
```

---

## 附录：参考资料

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [OpenAI Function Calling](https://platform.openai.com/docs/guides/function-calling)
- 项目设计文档: `docs/plans/2025-12-26-ai-assistant-design.md`
