CREATE TABLE IF NOT EXISTS ai_monitoring_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '用户ID',
    model_name VARCHAR(50) NOT NULL COMMENT '模型名称(deepseek-chat/LongCat-Flash-Chat)',
    call_type VARCHAR(20) NOT NULL COMMENT '调用类型(CHAT/SUGGESTION)',

    -- 性能指标
    start_time DATETIME NOT NULL COMMENT '调用开始时间',
    end_time DATETIME NOT NULL COMMENT '调用结束时间',
    latency_ms BIGINT NOT NULL COMMENT '响应耗时(毫秒)',

    -- Token使用量
    prompt_tokens INT DEFAULT NULL COMMENT '输入token数',
    completion_tokens INT DEFAULT NULL COMMENT '输出token数',
    total_tokens INT DEFAULT NULL COMMENT '总token数',

    -- 调用状态
    status VARCHAR(20) NOT NULL COMMENT '调用状态(SUCCESS/FAILURE/TIMEOUT)',
    error_type VARCHAR(100) DEFAULT NULL COMMENT '错误类型',
    error_message TEXT DEFAULT NULL COMMENT '错误详情',

    -- 请求/响应摘要
    user_message_preview VARCHAR(500) DEFAULT NULL COMMENT '用户消息摘要',
    assistant_message_preview VARCHAR(500) DEFAULT NULL COMMENT 'AI响应摘要',

    -- 元数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',

    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_model_name (model_name),
    INDEX idx_start_time (start_time),
    INDEX idx_status (status),
    INDEX idx_user_time (user_id, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用监控日志表';