-- AI会话元数据表
CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64) UNIQUE NOT NULL COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL COMMENT '会话标题',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_conversation (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话元数据表';
