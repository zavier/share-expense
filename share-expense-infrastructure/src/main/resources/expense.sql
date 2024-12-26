use share_expense;

DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `expense_project`;
DROP TABLE IF EXISTS `expense_project_member`;
DROP TABLE IF EXISTS `expense_record`;
DROP TABLE IF EXISTS `expense_record_consumer`;

-- 用户表 (user)
CREATE TABLE user (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    user_name VARCHAR(255) NOT NULL COMMENT '用户名',
    email VARCHAR(255) NOT NULL COMMENT '电子邮件',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    open_id VARCHAR(255) COMMENT '微信openId',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='用户信息表';

-- 费用项目表 (expense_project)
CREATE TABLE expense_project (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '费用项目ID',
    name VARCHAR(100) NOT NULL COMMENT '项目名称',
    description TEXT COMMENT '项目描述',
    create_user_id INT NOT NULL COMMENT '创建者用户ID',
    locked tinyint NOT NULL DEFAULT '0' COMMENT '是否锁定',
    version INT NOT NULL DEFAULT '0' COMMENT '版本号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='费用项目信息表';

-- 费用项目成员表 (expense_project_member)
CREATE TABLE expense_project_member (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id INT NOT NULL COMMENT '所属费用项目ID',
    name VARCHAR(255) NOT NULL COMMENT '成员名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='费用项目成员关联表';

-- 费用记录表 (expense_record)
CREATE TABLE expense_record (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '费用记录ID',
    project_id INT NOT NULL COMMENT '费用项目ID',
    pay_member VARCHAR(255) NOT NULL COMMENT '支付的用户名称',
    amount DECIMAL(10, 2) NOT NULL COMMENT '费用金额',
    pay_date DATE NOT NULL COMMENT '支付日期',
    expense_type VARCHAR(30) NOT NULL COMMENT '费用类型',
    remark varchar(300) NOT NULL DEFAULT '' COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='费用记录信息表';

-- 费用记录消费人员表
CREATE TABLE expense_record_consumer (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    project_id INT NOT NULL COMMENT '费用项目ID',
    record_id INT NOT NULL COMMENT '费用记录ID',
    member VARCHAR(255) NOT NULL COMMENT '消费成员名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='费用消费人员信息表';