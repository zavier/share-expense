package com.github.zavier.ai;

import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI模块集成测试
 *
 * 注意：由于AI模块依赖完整的Spring Boot上下文和数据库配置，
 * 完整的端到端集成测试应该在start模块中进行。
 * 此测试类主要用于验证DTO和基础组件的定义。
 */
class AiChatIntegrationTest {

    @Test
    void testAiChatRequest() {
        AiChatRequest request = new AiChatRequest("创建一个测试项目", "test-conv-123");

        assertNotNull(request.message());
        assertEquals("创建一个测试项目", request.message());
        assertEquals("test-conv-123", request.conversationId());
    }

    @Test
    void testAiChatResponse() {
        AiChatResponse response = AiChatResponse.builder()
            .conversationId("test-conv-123")
            .reply("好的，我来帮您创建项目")
            .build();

        assertNotNull(response.conversationId());
        assertEquals("test-conv-123", response.conversationId());
        assertEquals("好的，我来帮您创建项目", response.reply());
    }

    @Test
    void testAiChatRequestWithoutConversationId() {
        AiChatRequest request = new AiChatRequest("添加成员");

        assertNotNull(request.message());
        assertNull(request.conversationId());
        assertEquals("添加成员", request.message());
    }
}
