# 记账分摊软件说明

> 背景：平时朋友聚会、结伴游玩，可能不都是一个人出钱，大家在AA时这些花销自己算起来比较麻烦，写了一个简单的工具

后端使用 COLA 搭建


前端使用 amis

## 项目管理
1. 注册账号
2. 记账前需要创建一个项目，项目中可以有添加用户(可以是虚拟用户)
3. 用户可以记录每一笔花销，包括金额、日期、支付者、费用类型型、使用人等信息


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
