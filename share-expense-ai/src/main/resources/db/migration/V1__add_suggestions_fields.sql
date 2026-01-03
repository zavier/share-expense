-- 添加建议缓存字段到 AI 对话表
-- Migration: Add suggestions caching fields to ai_conversation and ai_chat_session tables

-- 为 ai_conversation 表添加建议字段
ALTER TABLE ai_conversation
ADD COLUMN suggestions JSON COMMENT '建议内容，JSON数组格式，存储序列化的 SuggestionItem 列表',
ADD COLUMN suggestions_updated_at DATETIME COMMENT '建议更新时间',
ADD COLUMN suggestions_generating TINYINT(1) DEFAULT 0 COMMENT '是否正在生成建议 (0-否, 1-是)';

-- 为 ai_chat_session 表添加建议字段
ALTER TABLE ai_chat_session
ADD COLUMN last_suggestions JSON COMMENT '最后一次建议内容，JSON数组格式',
ADD COLUMN suggestions_updated_at DATETIME COMMENT '建议更新时间',
ADD COLUMN suggestions_generating TINYINT(1) DEFAULT 0 COMMENT '是否正在生成建议 (0-否, 1-是)';

-- 添加索引以提高查询性能
CREATE INDEX idx_conv_suggestions_updated ON ai_conversation(suggestions_updated_at);
CREATE INDEX idx_conv_generating ON ai_conversation(suggestions_generating);
CREATE INDEX idx_session_suggestions_updated ON ai_chat_session(suggestions_updated_at);
CREATE INDEX idx_session_generating ON ai_chat_session(suggestions_generating);
