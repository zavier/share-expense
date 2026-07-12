# 更新日志

## v2.3 (2026-07-12)

- ✨ 新增 **Docker 多阶段构建**（Maven 构建 + JRE 运行）
- ✨ 新增 **Docker Compose** 一键部署（应用 + MySQL，自动建库建表）
- ✨ 新增 `.dockerignore` 优化构建上下文
- 📝 更新 README Docker 部署文档

## v2.2 (2026-07-11)

- ✨ 新增 **GitHub Actions CI** 工作流（自动构建 + 测试）
- ✨ 迁移测试框架：JUnit 4 → **JUnit 5**
- ✨ 合并 Service-Executor 透传层为 DDD **ApplicationService**
- 📝 新增 ADR-0001：ApplicationService 保留合并决策记录
- ♻️ SecurityContext 从 Web Adapter 分离

## v2.1 (2026-01 ~ 2026-07)

- ✨ 升级 LLM 模型：**DeepSeek V4 Flash** + **LongCat 2.0**
- ✨ 新增 **AI 调用监控系统**（性能统计、错误分析）
- ✨ 升级 Spring AI 至 **1.1.2**
- ✨ 多模型提供者支持

## v2.0 (2025-01) - AI 助手

- ✨ 基于 **Anthropic 最佳实践**优化 AI 工具函数
- ✨ Token 效率提升 **60%**（平均 1250 → 500 tokens/对话）
- ✨ 支持自然语言项目名称识别（无需 ID）
- ✨ 响应格式控制（concise/detailed 模式）
- ✨ 统一 `Expense*` 工具函数前缀

## v1.5 (2024-12-30)

- ✨ 数据访问层从 MyBatis 迁移至 **Spring Data JPA**
- ✨ 添加乐观锁支持

## v1.0 (2024-12-20)

- ✨ 初始版本发布（Web 端、小程序、AI 助手、基础费用分摊）
