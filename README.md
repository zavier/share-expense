# 费用分摊管理系统 (Share Expense)

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![COLA](https://img.shields.io/badge/COLA-4.3.2-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

一个简单易用的费用分摊管理工具，帮助朋友间、旅行伙伴间轻松记录和分摊费用。

[在线体验](https://zhengw-tech.com/expense/index.html) • [AI助手](https://zhengw-tech.com/expense/ai-chat.html) • [功能文档](#功能特性)

</div>

---

## 📖 项目简介

在日常生活中，朋友聚会、结伴出游等场景下，费用分摊计算往往令人头疼。本系统旨在解决这个问题，提供：

- ✅ **简单的费用记录**：记录每笔支出的付款人、金额、参与人
- ✅ **自动分摊计算**：自动计算每个人应付/应收的金额
- ✅ **多端支持**：Web端、微信小程序、AI助手
- ✅ **智能结算**：一键查看项目结算情况

---

## ✨ 功能特性

### 💰 核心功能

- **项目管理**：创建不同的费用项目（如"周末聚餐"、"云南旅行"）
- **成员管理**：添加项目成员，记录参与人员
- **费用记录**：记录每笔支出，支持多种费用类型
- **智能结算**：自动计算每个人的应付/应收金额
- **数据导出**：支持导出费用明细（Excel格式）

### 🤖 AI助手 ⭐

基于 **Spring AI** 的智能对话助手，支持自然语言交互：

#### 功能亮点
- 🗣️ **自然语言交互**：像聊天一样使用系统
  - "帮我把周末聚餐的结算算一下"
  - "记录一笔支出，Alice付了50元吃饭，我们3个人AA"

- 🔍 **智能查询**：
  - 项目查询（支持模糊搜索）
  - 费用统计（按类型、按成员）
  - 结算计算

- ⚡ **快速操作**：
  - 创建项目
  - 添加成员
  - 记录费用

#### 技术特点
- 基于 **Anthropic 最佳实践**优化 AI 工具函数
- **Token 效率优化**：平均节省 60% tokens
- 支持**项目名称**自动识别（无需记住ID）
- **灵活的响应格式**：精简模式 vs 详细模式

### 📱 多端支持

| 端 | 技术栈 | 访问方式 |
|---|--------|---------|
| **Web端** | amis | [在线体验](https://zhengw-tech.com/expense/index.html) |
| **AI助手** | Spring AI + Claude | [AI聊天](https://zhengw-tech.com/expense/ai-chat.html) |
| **微信小程序** | 微信小程序 | 扫描下方二维码 |

![小程序二维码](https://zhengw-tech.com/images/expense/share_expense_qrcode.jpg)

---

## 🏗️ 技术架构

### 技术栈

**后端**
- **Java 21**：现代Java特性
- **Spring Boot 3.2.0**：应用框架
- **COLA 4.3.2**：Clean Object-Oriented and Layered Architecture
- **Spring Data JPA**：数据持久化
- **Spring AI 1.0.0-M4**：AI集成
- **MySQL 8.0+**：数据库
- **EasyExcel 3.3.4**：Excel处理

**前端**
- **amis**：低代码前端框架
- **微信小程序**：原生小程序开发

### 架构设计

项目采用 **COLA架构**（Clean Object-Oriented and Layered Architecture）：

```
share-expense/
├── share-expense-client/       # 客户端层（DTO、API接口定义）
├── share-expense-adapter/      # 适配器层（REST Controller、小程序适配器）
├── share-expense-app/          # 应用层（业务逻辑编排）
├── share-expense-domain/       # 领域层（核心业务规则）
├── share-expense-infrastructure/  # 基础设施层（数据访问、外部服务）
├── share-expense-ai/           # AI助手模块 ⭐
└── start/                      # 启动模块
```

**核心设计模式**：
- **CQRS**：命令查询分离
- **Gateway Pattern**：领域层定义网关接口
- **Repository Pattern**：Spring Data JPA
- **DTO Pattern**：数据传输对象

---

## 🚀 快速开始

### 环境要求

- **JDK**: 21 或更高版本
- **Maven**: 3.6.0 或更高版本
- **MySQL**: 8.0 或更高版本
- **IDE**: IntelliJ IDEA（推荐）

### 数据库配置

1. **创建数据库**

```sql
CREATE DATABASE share_expense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **导入初始化脚本**

```bash
mysql -u root -p share_expense < share-expense-infrastructure/src/main/resources/expense.sql
```

3. **配置数据源**

编辑 `start/src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/share_expense
spring.datasource.username=root
spring.datasource.password=your_password
```

### 运行应用

```bash
# 克隆项目
git clone https://github.com/zavier/share-expense.git
cd share-expense

# 编译项目
mvn clean compile

# 运行应用
cd start && mvn spring-boot:run
```

访问：http://localhost:8081

### 配置AI助手（可选）

如需使用AI助手功能，需要配置 OpenAI API Key 或DeepSeek等：

```bash
export OPENAI_API_KEY=your-api-key-here
```

或编辑 `start/src/main/resources/application.properties`：

```properties
spring.ai.openai.api-key=your-api-key-here
spring.ai.openai.base-url=https://api.openai.com
spring.ai.openai.chat.options.model=gpt-4o-mini
```

---

## 📦 部署指南

### 打包部署

```bash
# 打包
mvn clean package -DskipTests

# 运行
java -jar start/target/start-1.0.0-SNAPSHOT.jar
```

### Docker部署（可选）

```dockerfile
FROM openjdk:21-jre-slim
COPY target/start-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## 📚 开发指南

### 项目结构

```
share-expense/
├── docs/                    # 项目文档
│   └── plans/              # 设计方案和优化计划
├── share-expense-client/    # 客户端层
│   └── dto/                # DTO对象
├── share-expense-adapter/   # 适配器层
│   ├── web/                # Web控制器
│   └── wx/                 # 小程序适配器
├── share-expense-app/       # 应用层
│   └── executor/           # 命令/查询执行器
├── share-expense-domain/    # 领域层
│   ├── core/               # 核心领域对象
│   ├── gateway/            # 网关接口
│   └── service/            # 领域服务
├── share-expense-infrastructure/  # 基础设施层
│   └── repository/         # 数据访问实现
├── share-expense-ai/        # AI助手模块 ⭐
│   ├── function/           # AI工具函数
│   ├── service/            # AI服务
│   └── dto/                # AI DTO
└── start/                   # 启动模块
    └── resources/          # 配置文件
```

### 命名规范

- **Command类**: `*AddCmd`, `*UpdateCmd`, `*DeleteCmd`
- **Query类**: `*ListQry`, `*DetailQry`
- **Executor类**: `*CmdExe`, `*QryExe`
- **Domain类**: 业务领域对象
- **DO类**: JPA实体对象（`*DO.java`）
- **Repository类**: Spring Data JPA接口（`*Repository.java`）

### 测试

```bash
# 运行所有测试
mvn test

# 运行集成测试
mvn failsafe:integration-test

# 运行单个测试类
mvn test -Dtest=ExpenseProjectGatewayImplTest
```

---

## 🔄 更新日志

### v2.0 (2025-01-02) - AI助手优化 ⭐

**重大更新**：
- ✨ 基于 **Anthropic 最佳实践**优化AI工具函数
- ✨ Token效率提升 **60%**（平均1250 → 500 tokens/对话）
- ✨ 支持**自然语言项目名称**（无需ID）
- ✨ 新增**响应格式控制**（concise/detailed模式）
- ✨ 增强**错误提示和建议**

**AI工具重构**：
- 重命名所有工具函数（统一`Expense*`前缀）
- 删除重复的ById/ByName方法
- 合并`GetProjectDetails`到`ListProjects`
- 拆分`GetExpenseDetails`为可控的summary/records/all模式

**新增组件**：
- `ExpenseResponseFormat`：响应格式枚举
- `ExpenseDetailSection`：查询内容枚举
- `ProjectIdentifierResolver`：智能标识符解析器
- `BaseExpenseFunction`：统一基类

详见：[AI优化方案v2.0](docs/plans/2025-01-02-ai-function-optimization-plan-v2.md)

### v1.5 (2024-12-30)

- ✨ 迁移数据访问层从 MyBatis 到 **Spring Data JPA**
- ✨ 优化实体关系和级联操作
- ✨ 添加乐观锁支持
- ✨ 增加测试覆盖率

### v1.0 (2024-12-20)

- ✨ 初始版本发布
- ✨ 支持Web端、小程序、AI助手
- ✨ 基础费用分摊功能

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

### 代码规范

- 遵循 **Google Java Style Guide**
- 使用 **Lombok** 简化代码
- 添加适当的**单元测试**和**集成测试**
- 更新相关**文档**

---


<div align="center">

**如果这个项目对您有帮助，请给一个 ⭐Star 支持一下！**

Made with ❤️ by zavier

</div>
