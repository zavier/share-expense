# 记账分摊软件说明

> 背景：平时朋友聚会、结伴游玩，可能不都是一个人出钱，大家在AA时这些花销自己算起来比较麻烦，写了一个简单的工具

后端使用 COLA 搭建


前端使用 amis

## 用例
- 创建项目
- 删除项目
- 增加项目成员
- 锁定项目
- 增加费用记录
- 删除费用记录




### 项目管理
1. 只有注册登录后才能创建项目
2. 可以查看自己创建的项目
3. 管理员可以查看对应的项目
4. 管理员可以复制项目（不包含费用信息）
5. 只有创建人可以锁定项目，锁定后不能新增成员及费用

### 成员管理
1. 管理员可以为项目添加成员（可以为虚拟成员，非登录账号）
2. 可以创建家庭组，将成员归类

### 费用管理
1. 管理员可以添加费用信息（金额、日期、支付者、费用类型型、消费人等信息）

### 账单管理
1. 支持消费人员均摊费用，生成均摊费用账单
2. 支持家庭维度账单和个人维度账单
3. 支持账单导出



## 业务规则

| 规则编号 | 模块     | 规则描述 | 举例 | 影响的主要功能 |
| -------- | -------- | -------- | ---- | -------------- |
|          | 项目管理 |          |      |                |
|          | 成员管理 |          |      |                |
|          | 费用管理 |          |      |                |
|          | 账单管理 |          |      |                |



## 词汇表

| 分类 | 中文 | 英文 | 英文简称 |
| ---- | ---- | ---- | -------- |
|      |      |      |          |
|      |      |      |          |
|      |      |      |          |





## 使用

启动项目，访问 http://localhost:8080/ 即可，部分截图如下

### 项目列表
自己创建或者加入的项目
![](https://raw.githubusercontent.com/zavier/share-expense/main/img/projectList.png)

### 添加项目成员
![](https://raw.githubusercontent.com/zavier/share-expense/main/img/addMember.png)

### 添加费用信息
![](https://raw.githubusercontent.com/zavier/share-expense/main/img/addRecord.png)

### 全部费用明细
![](https://raw.githubusercontent.com/zavier/share-expense/main/img/recordInfo.png)

### 费用分摊明细
![](https://raw.githubusercontent.com/zavier/share-expense/main/img/shareDetail.png)
