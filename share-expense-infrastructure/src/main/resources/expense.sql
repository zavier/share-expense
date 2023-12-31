
-- 用户表 (user)
CREATE TABLE user (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(255) NOT NULL COMMENT '用户名',
    email VARCHAR(255) NOT NULL COMMENT '电子邮件',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='用户信息表';

-- 费用项目表 (expense_project)
CREATE TABLE expense_project (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '费用项目ID',
    name VARCHAR(100) NOT NULL COMMENT '项目名称',
    description TEXT COMMENT '项目描述',
    create_user_id INT NOT NULL COMMENT '创建者用户ID',
    version INT NOT NULL DEFAULT '0' COMMENT '版本号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='费用项目信息表';

-- 费用项目成员表 (expense_project_member)
CREATE TABLE expense_project_member (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    project_id INT NOT NULL COMMENT '所属费用项目ID',
    user_id INT NOT NULL COMMENT '用户ID',
    user_name VARCHAR(255) NOT NULL COMMENT '用户名',
    weight INT NOT NULL DEFAULT '1' COMMENT '均摊权重(人份)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='费用项目成员关联表';

-- 费用记录表 (expense_record)
CREATE TABLE expense_record (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '费用记录ID',
    project_id INT NOT NULL COMMENT '费用项目ID',
    cost_user_id INT NOT NULL COMMENT '花费的用户ID',
    cost_user_name VARCHAR(255) NOT NULL COMMENT '花费的用户名称',
    amount DECIMAL(10, 2) NOT NULL COMMENT '费用金额',
    date DATE NOT NULL COMMENT '费用日期',
    expense_type VARCHAR(30) NOT NULL COMMENT '费用类型',
    remark varchar(300) NOT NULL DEFAULT '' COMMENT '备注',
    version INT NOT NULL DEFAULT '0' COMMENT '版本号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='费用记录信息表';

-- 费用均摊记录表 (expense_sharing)
CREATE TABLE expense_sharing (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '费用均摊记录ID',
    record_id INT NOT NULL COMMENT '费用记录ID',
    user_id INT NOT NULL COMMENT '用户ID',
    user_name VARCHAR(255) NOT NULL COMMENT '用户名',
    weight INT NOT NULL DEFAULT '1' COMMENT '均摊权重',
    amount DECIMAL(10, 2) COMMENT '均摊金额(人份)',
    is_paid TINYINT NOT NULL DEFAULT '0' COMMENT '是否已支付，0:否 1:是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='费用均摊记录表';
