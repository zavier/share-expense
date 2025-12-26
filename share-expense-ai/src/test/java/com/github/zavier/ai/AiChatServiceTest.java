package com.github.zavier.ai;

import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiChatServiceTest {

    // TODO: 添加 Mock 和实际测试用例
    // 由于需要真实的 OpenAI API 连接,建议使用 MockMvc 或 WireMock 进行测试

    @Test
    void testChatRequest() {
        AiChatRequest request = new AiChatRequest();
        request.setMessage("创建一个测试项目");

        assertNotNull(request.getMessage());
        assertEquals("创建一个测试项目", request.getMessage());
    }

    @Test
    void testChatResponse() {
        AiChatResponse response = AiChatResponse.builder()
            .conversationId("test-conv-123")
            .reply("好的,我来帮您创建项目")
            .build();

        assertEquals("test-conv-123", response.getConversationId());
        assertEquals("好的,我来帮您创建项目", response.getReply());
    }

    @Test
    void testChatRequestWithConversationId() {
        AiChatRequest request = new AiChatRequest();
        request.setMessage("添加成员");
        request.setConversationId("existing-conv-456");

        assertNotNull(request.getMessage());
        assertNotNull(request.getConversationId());
        assertEquals("添加成员", request.getMessage());
        assertEquals("existing-conv-456", request.getConversationId());
    }

    @Test
    void testChatResponseWithPendingAction() {
        AiChatResponse response = AiChatResponse.builder()
            .conversationId("test-conv-789")
            .reply("即将创建项目,请确认")
            .pendingAction(null)
            .build();

        assertEquals("test-conv-789", response.getConversationId());
        assertEquals("即将创建项目,请确认", response.getReply());
        assertNull(response.getPendingAction());
    }
}
