# 费用分摊管理系统 (Share Expense)

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![COLA](https://img.shields.io/badge/COLA-4.3.2-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

一个简单易用的费用分摊管理工具，帮助朋友间、旅行伙伴间轻松记录和分摊费用。

[在线体验](https://zhengw-tech.com/expense/index.html) • [AI助手](https://zhengw-tech.com/expense/ai-chat.html)

</div>

---

## 📖 项目简介

在日常生活中，朋友聚会、结伴出游等场景下，费用分摊计算往往令人头疼。本系统提供：

- ✅ **简单的费用记录**：记录每笔支出的付款人、金额、参与人
- ✅ **自动分摊计算**：自动计算每个人应付/应收的金额
- ✅ **多端支持**：Web 端、微信小程序、AI 助手
- ✅ **智能结算**：一键查看项目结算情况

---

## ✨ 功能特性

### 💰 核心功能

- **项目管理**：创建不同的费用项目（如"周末聚餐"、"云南旅行"）
- **成员管理**：添加项目成员，记录参与人员
- **费用记录**：记录每笔支出，支持多种费用类型
- **智能结算**：自动计算每个人的应付/应收金额
- **数据导出**：支持导出费用明细（Excel 格式）

### 🤖 AI 助手

基于 **Spring AI** 的智能对话助手，支持自然语言交互：

- 🗣️ **自然语言交互**：像聊天一样使用系统
  - "帮我把周末聚餐的结算算一下"
  - "记录一笔支出，Alice 付了 50 元吃饭，我们 3 个人 AA"
- 🔍 **智能查询**：项目查询、费用统计、结算计算
- ⚡ **快速操作**：创建项目、添加成员、记录费用

**模型配置**：

| 模型 | 用途 | API Key |
|------|------|---------|
| DeepSeek V4 Flash | 主对话模型 | `DEEPSEEK_API_KEY` |
| LongCat 2.0 | 快速建议生成 | `LONGCAT_API_KEY` |

### 📊 AI 监控

AI 调用监控系统，提供调用历史查询和性能统计：

- 📋 **调用历史**：按会话查询 AI 调用记录，支持按类型、状态筛选
- 📈 **性能统计**：平均延迟、P50/P90/P99 百分位数、成功率、Token 用量
- 🔍 **错误分析**：按错误类型分组统计、错误趋势分析

**API 接口**：`GET /api/ai/monitoring/session/{conversationId}/history`

### 📱 多端支持

| 端 | 技术栈 | 访问方式 |
|---|--------|---------|
| **Web 端** | amis 低代码框架 | [在线体验](https://zhengw-tech.com/expense/index.html) |
| **AI 助手** | Spring AI + DeepSeek | [AI 聊天](https://zhengw-tech.com/expense/ai-chat.html) |
| **微信小程序** | 原生小程序 | 扫描下方二维码 |

![小程序二维码](https://zhengw-tech.com/images/expense/share_expense_qrcode.png)

---

## 🏗️ 技术架构

### 技术栈

**后端**
- **Java 21**：现代 Java 特性
- **Spring Boot 3.2.0**：应用框架
- **COLA 4.3.2**：Clean Object-Oriented and Layered Architecture
- **Spring Data JPA**：数据持久化
- **Spring AI 1.1.2**：AI 集成
- **MySQL 8.0+**：数据库
- **EasyExcel 3.3.4**：Excel 处理
- **jjwt 0.12.5**：JWT 认证

**前端**
- **amis**：低代码前端框架
- **微信小程序**：原生小程序开发

**DevOps**
- **Docker**：多阶段构建 + Compose 一键部署
- **GitHub Actions**：CI 自动构建和测试

### 架构设计

项目采用 **COLA 架构**（Clean Object-Oriented and Layered Architecture）：

```
share-expense/
├── share-expense-client/          # 客户端层（DTO、API 接口定义）
├── share-expense-adapter/         # 适配器层（REST Controller、小程序适配器）
├── share-expense-app/             # 应用层（ApplicationService 业务编排）
├── share-expense-domain/          # 领域层（核心业务规则、网关接口）
├── share-expense-infrastructure/  # 基础设施层（JPA 仓库、数据访问）
├── share-expense-ai/              # AI 助手模块
└── start/                         # 启动模块
```

**核心设计模式**：
- **CQRS**：命令查询职责分离
- **Gateway Pattern**：领域层定义网关接口，基础设施层实现
- **Repository Pattern**：Spring Data JPA
- **DTO Pattern**：数据传输对象

---

## 🚀 快速开始

### 环境要求

- **JDK**：21+
- **Maven**：3.6+
- **MySQL**：8.0+
- **IDE**：IntelliJ IDEA（推荐）

### 方式一：Docker Compose（推荐）

一键启动应用 + MySQL 数据库，无需本地安装 JDK/Maven：

```bash
# 克隆项目
git clone https://github.com/zavier/share-expense.git
cd share-expense

# 启动所有服务
docker compose up -d

# 查看日志
docker compose logs -f app
```

启动后访问：http://localhost:8081

> 首次启动自动执行建表脚本，数据通过 `mysql-data` 卷持久化。数据库密码默认 `secret123`，可在 `docker-compose.yml` 中修改。

### 方式二：本地开发

**1. 数据库配置**

```sql
CREATE DATABASE share_expense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -u root -p share_expense < share-expense-infrastructure/src/main/resources/expense.sql
```

编辑 `start/src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/share_expense
spring.datasource.username=root
spring.datasource.password=${MYSQL_PWD}
```

**2. 配置 AI 助手（可选）**

```bash
export DEEPSEEK_API_KEY=your-deepseek-api-key
export LONGCAT_API_KEY=your-longcat-api-key
```

**3. 运行应用**

```bash
mvn clean compile
cd start && mvn spring-boot:run
```

访问：http://localhost:8081

---

## 📦 部署指南

### 打包部署

```bash
mvn clean package -DskipTests
java -jar start/target/start-1.0.0-SNAPSHOT.jar
```

### Docker 仅构建镜像

```bash
# 构建镜像（多阶段构建，无需本地 JDK/Maven）
docker build -t share-expense .

# 运行容器（需自行提供 MySQL）
docker run -d -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/share_expense \
  -e MYSQL_PWD=your_password \
  share-expense
```

---

## 📚 开发指南

### 项目结构

```
share-expense/
├── docs/
│   ├── adr/                  # 架构决策记录
│   └── agents/               # Agent 配置文档
├── share-expense-client/     # 客户端层
│   └── dto/                  # CQRS 命令/查询对象 & 数据传输对象
├── share-expense-adapter/    # 适配器层
│   ├── web/                  # REST 控制器
│   └── wx/                   # 小程序适配器
├── share-expense-app/        # 应用层
│   ├── project/              # 费用项目 ApplicationService
│   └── user/                 # 用户 ApplicationService
├── share-expense-domain/     # 领域层
│   ├── core/                 # 核心领域对象
│   ├── gateway/              # 网关接口
│   └── service/              # 领域服务
├── share-expense-infrastructure/  # 基础设施层
│   ├── expense/              # 费用记录 JPA 实体和仓库
│   ├── project/              # 项目 JPA 实体和网关实现
│   └── user/                 # 用户 JPA 实体和网关实现
├── share-expense-ai/         # AI 助手模块
│   ├── function/             # AI 工具函数（@Tool 注解）
│   ├── service/              # AI 服务（ChatModelProvider 等）
│   ├── monitoring/           # AI 调用监控
│   ├── resolver/             # 项目标识符解析器
│   └── controller/           # AI 对话和会话 REST 接口
└── start/                    # 启动模块
    └── resources/            # 配置文件
```

### 命名规范

- **Command 类**：`*AddCmd`、`*UpdateCmd`、`*DeleteCmd`
- **Query 类**：`*ListQry`、`*DetailQry`
- **ApplicationService**：`*ApplicationService`
- **Domain 类**：业务领域对象
- **DO 类**：JPA 实体对象（`*DO.java`）
- **Repository 类**：Spring Data JPA 接口（`*Repository.java`）

### 测试

```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=ExpenseProjectGatewayImplTest
```

---

## 🔄 更新日志

详见 [CHANGELOG.md](CHANGELOG.md)

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
- 添加适当的单元测试
- 更新相关文档

---

<div align="center">

**如果这个项目对您有帮助，请给一个 ⭐Star 支持一下！**

Made with ❤️ by zavier

</div>
