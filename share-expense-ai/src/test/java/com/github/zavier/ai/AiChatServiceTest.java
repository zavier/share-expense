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
        AiChatRequest request = new AiChatRequest("创建一个测试项目");

        assertNotNull(request.message());
        assertEquals("创建一个测试项目", request.message());
    }

    @Test
    void testChatResponse() {
        AiChatResponse response = AiChatResponse.builder()
            .conversationId("test-conv-123")
            .reply("好的,我来帮您创建项目")
            .build();

        assertEquals("test-conv-123", response.conversationId());
        assertEquals("好的,我来帮您创建项目", response.reply());
    }

    @Test
    void testChatRequestWithConversationId() {
        AiChatRequest request = new AiChatRequest("添加成员", "existing-conv-456");

        assertNotNull(request.message());
        assertNotNull(request.conversationId());
        assertEquals("添加成员", request.message());
        assertEquals("existing-conv-456", request.conversationId());
    }

    @Test
    void testChatResponseBasic() {
        AiChatResponse response = AiChatResponse.builder()
            .conversationId("test-conv-789")
            .reply("即将创建项目,请确认")
            .build();

        assertEquals("test-conv-789", response.conversationId());
        assertEquals("即将创建项目,请确认", response.reply());
    }
}
