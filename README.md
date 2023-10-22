# 记账分摊软件说明

> 背景：平时朋友聚会，可能不都是一个人出钱，大家在AA时这些花销自己算起来比较麻烦，写了一个简单的工具

后端使用 COLA 搭建


前端使用 amis

## 项目管理
1. 每个人需要创建一个自己的账号
2. 记账前需要创建一个项目，项目中可以有一个或多个用户
3. 每个用户有一个分摊权重(比如有人出两个人份的钱)
4. 用户可以记录每一笔花销，包括金额、日期、支付者、花销类型等信息
5. 项目中的费用按照分摊权重，所有人进行分摊


使用

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
