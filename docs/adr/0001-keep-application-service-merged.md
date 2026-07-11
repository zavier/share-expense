# ADR-0001: ApplicationService 合并 — 保留而非重新拆分为 Executor

## 背景

PR [#13](https://github.com/zavier/share-expense/pull/13)（commit `f7df51b`）将 11 个 COLA Executor 合并为 2 个 ApplicationService（`ExpenseApplicationService` + `UserApplicationService`），净减少 ~1,229 行代码。理由是：每个 Executor 只做 `validate → gateway.get → gateway.save` 的三步透传，删除它们通过了删除测试——复杂性没有扩散到调用方，而是随 Executor 一起消失了。

2026-07 的架构审查（`/improve-codebase-architecture`，issue [#14](https://github.com/zavier/share-expense/issues/14)）提出将 `ExpenseApplicationService` 重新拆分为 Executor，理由是合并后的单类有 15 个方法但零测试覆盖，缺乏 locality。

## 决策

**保留合并**。上次的合并方向是正确的。当前的问题不是缺少 Executor 层，而是：

1. **`ExpenseApplicationService` 零测试覆盖**——但它在当前形态下就是可测的：注入 mock `ExpenseProjectGateway` 和 `ExpenseRecordValidator` 即可测试每个方法
2. **领域模型还不够深**——validate → load → save 的模式重复出现，说明领域实体（`ExpenseProject`、`ExpenseRecord`）本身承担的行为太少。更多逻辑应该下沉到领域模型中，而不是在 ApplicationService 或 Executor 层中展开

## 替代方案（已否决）

**拆回 COLA Executor**（每个命令/查询一个 Executor 类）。否决理由：上次合并已经证明 Executor 是透传层。拆回去会增加 ~800+ 行的 boilerplate（接口 + 实现 + Spring 注册），但不改变任何业务逻辑的结构。真正需要的是给现有类加测试，并加深领域模型。

## 后果

- `ExpenseApplicationService` 保持当前形态，补齐测试覆盖
- 领域模型深化（`ExpenseProject` 承担更多行为、减少 service 层展开）作为后续迭代方向
- 如果未来某个操作确实积累了足够复杂的逻辑（不再是三步透传），届时再为它单独提取更深模块——以真实复杂性为信号，而非以 COLA 规范为准则
