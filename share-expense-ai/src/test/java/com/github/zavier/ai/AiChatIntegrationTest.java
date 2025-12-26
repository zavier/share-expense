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
        AiChatRequest request = new AiChatRequest();
        request.setMessage("创建一个测试项目");
        request.setConversationId("test-conv-123");

        assertNotNull(request.getMessage());
        assertEquals("创建一个测试项目", request.getMessage());
        assertEquals("test-conv-123", request.getConversationId());
    }

    @Test
    void testAiChatResponse() {
        AiChatResponse response = AiChatResponse.builder()
            .conversationId("test-conv-123")
            .reply("好的，我来帮您创建项目")
            .build();

        assertNotNull(response.getConversationId());
        assertEquals("test-conv-123", response.getConversationId());
        assertEquals("好的，我来帮您创建项目", response.getReply());
        assertNull(response.getPendingAction());
    }

    @Test
    void testAiChatResponseWithPendingAction() {
        AiChatResponse response = AiChatResponse.builder()
            .conversationId("test-conv-456")
            .reply("请确认以下操作")
            .pendingAction(new com.github.zavier.ai.dto.PendingAction())
            .build();

        assertNotNull(response.getConversationId());
        assertNotNull(response.getPendingAction());
    }
}
