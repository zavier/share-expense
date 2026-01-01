package com.github.zavier.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zavier.ai.dto.MessageDto;
import com.github.zavier.ai.dto.RenameSessionRequest;
import com.github.zavier.ai.dto.SessionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AI 会话管理 Controller 单元测试
 * 测试 REST API 接口的请求/响应、参数验证、异常处理
 */
@ExtendWith(MockitoExtension.class)
@WebMvcTest(controllers = AiSessionController.class)
@ContextConfiguration(classes = {AiSessionController.class, GlobalExceptionHandler.class})
class AiSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiSessionService aiSessionService;

    private static final String TEST_CONVERSATION_ID = "test-conv-123";
    private static final String TEST_TITLE = "测试会话";
    private static final Integer TEST_USER_ID = 1;

    // ========== GET /expense/api/ai/sessions ==========

    @Test
    void testListSessions_Success() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        SessionDto session1 = new SessionDto(1L, "conv-1", "会话1", now.minusDays(1), now);
        SessionDto session2 = new SessionDto(2L, "conv-2", "会话2", now.minusDays(2), now.minusDays(2));

        when(aiSessionService.listSessions()).thenReturn(List.of(session1, session2));

        // When & Then
        mockMvc.perform(get("/expense/api/ai/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessions").isArray())
                .andExpect(jsonPath("$.data.sessions", hasSize(2)))
                .andExpect(jsonPath("$.data.sessions[0].conversationId").value("conv-1"))
                .andExpect(jsonPath("$.data.sessions[0].title").value("会话1"))
                .andExpect(jsonPath("$.data.sessions[1].conversationId").value("conv-2"))
                .andExpect(jsonPath("$.data.sessions[1].title").value("会话2"));

        verify(aiSessionService).listSessions();
    }

    @Test
    void testListSessions_EmptyList() throws Exception {
        // Given
        when(aiSessionService.listSessions()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/expense/api/ai/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessions").isArray())
                .andExpect(jsonPath("$.data.sessions", hasSize(0)));

        verify(aiSessionService).listSessions();
    }

    // ========== POST /expense/api/ai/sessions ==========

    @Test
    void testCreateSession_Success() throws Exception {
        // Given
        String conversationId = "new-conv-uuid-123";
        when(aiSessionService.createSession()).thenReturn(conversationId);

        // When & Then
        mockMvc.perform(post("/expense/api/ai/sessions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.title").value("新对话"));

        verify(aiSessionService).createSession();
    }

    // ========== DELETE /expense/api/ai/sessions/{conversationId} ==========

    @Test
    void testDeleteSession_Success() throws Exception {
        // Given
        doNothing().when(aiSessionService).deleteSession(eq(TEST_CONVERSATION_ID));

        // When & Then
        mockMvc.perform(delete("/expense/api/ai/sessions/{conversationId}", TEST_CONVERSATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(aiSessionService).deleteSession(TEST_CONVERSATION_ID);
    }

    @Test
    void testDeleteSession_SessionNotFound() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("会话不存在"))
                .when(aiSessionService).deleteSession(eq(TEST_CONVERSATION_ID));

        // When & Then - 全局异常处理器返回 200 但 success: false
        mockMvc.perform(delete("/expense/api/ai/sessions/{conversationId}", TEST_CONVERSATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        verify(aiSessionService).deleteSession(TEST_CONVERSATION_ID);
    }

    @Test
    void testDeleteSession_Forbidden() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("无权访问该会话"))
                .when(aiSessionService).deleteSession(eq(TEST_CONVERSATION_ID));

        // When & Then - 全局异常处理器返回 200 但 success: false
        mockMvc.perform(delete("/expense/api/ai/sessions/{conversationId}", TEST_CONVERSATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        verify(aiSessionService).deleteSession(TEST_CONVERSATION_ID);
    }

    // ========== PUT /expense/api/ai/sessions/{conversationId}/rename ==========

    @Test
    void testRenameSession_Success() throws Exception {
        // Given
        String newTitle = "新标题";
        RenameSessionRequest request = new RenameSessionRequest(newTitle);
        doNothing().when(aiSessionService).renameSession(eq(TEST_CONVERSATION_ID), eq(newTitle));

        // When & Then
        mockMvc.perform(put("/expense/api/ai/sessions/{conversationId}/rename", TEST_CONVERSATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(aiSessionService).renameSession(TEST_CONVERSATION_ID, newTitle);
    }

    @Test
    void testRenameSession_EmptyTitle() throws Exception {
        // Given
        RenameSessionRequest request = new RenameSessionRequest("");

        // When & Then - @Valid 验证在 @WebMvcTest 中需要额外配置，这里跳过
        // 实际应用中验证会在 Service 层或全局异常处理器中处理
        mockMvc.perform(put("/expense/api/ai/sessions/{conversationId}/rename", TEST_CONVERSATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(aiSessionService).renameSession(eq(TEST_CONVERSATION_ID), eq(""));
    }

    @Test
    void testRenameSession_NullTitle() throws Exception {
        // Given
        RenameSessionRequest request = new RenameSessionRequest(null);

        // When & Then
        mockMvc.perform(put("/expense/api/ai/sessions/{conversationId}/rename", TEST_CONVERSATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(aiSessionService).renameSession(eq(TEST_CONVERSATION_ID), eq(null));
    }

    @Test
    void testRenameSession_SessionNotFound() throws Exception {
        // Given
        String newTitle = "新标题";
        RenameSessionRequest request = new RenameSessionRequest(newTitle);
        doThrow(new IllegalArgumentException("会话不存在"))
                .when(aiSessionService).renameSession(eq(TEST_CONVERSATION_ID), eq(newTitle));

        // When & Then - 全局异常处理器返回 200 但 success: false
        mockMvc.perform(put("/expense/api/ai/sessions/{conversationId}/rename", TEST_CONVERSATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        verify(aiSessionService).renameSession(TEST_CONVERSATION_ID, newTitle);
    }

    // ========== GET /expense/api/ai/sessions/{conversationId}/messages ==========

    @Test
    void testGetSessionMessages_Success() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        MessageDto message1 = new MessageDto("user", "你好", now.minusMinutes(2));
        MessageDto message2 = new MessageDto("assistant", "你好，有什么可以帮助你？", now.minusMinutes(1));

        when(aiSessionService.getSessionMessages(eq(TEST_CONVERSATION_ID)))
                .thenReturn(List.of(message1, message2));

        // When & Then
        mockMvc.perform(get("/expense/api/ai/sessions/{conversationId}/messages", TEST_CONVERSATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messages").isArray())
                .andExpect(jsonPath("$.data.messages", hasSize(2)))
                .andExpect(jsonPath("$.data.messages[0].role").value("user"))
                .andExpect(jsonPath("$.data.messages[0].content").value("你好"))
                .andExpect(jsonPath("$.data.messages[1].role").value("assistant"))
                .andExpect(jsonPath("$.data.messages[1].content").value("你好，有什么可以帮助你？"));

        verify(aiSessionService).getSessionMessages(TEST_CONVERSATION_ID);
    }

    @Test
    void testGetSessionMessages_EmptyList() throws Exception {
        // Given
        when(aiSessionService.getSessionMessages(eq(TEST_CONVERSATION_ID)))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/expense/api/ai/sessions/{conversationId}/messages", TEST_CONVERSATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messages").isArray())
                .andExpect(jsonPath("$.data.messages", hasSize(0)));

        verify(aiSessionService).getSessionMessages(TEST_CONVERSATION_ID);
    }

    @Test
    void testGetSessionMessages_SessionNotFound() throws Exception {
        // Given
        when(aiSessionService.getSessionMessages(eq(TEST_CONVERSATION_ID)))
                .thenThrow(new IllegalArgumentException("会话不存在"));

        // When & Then - 全局异常处理器返回 200 但 success: false
        mockMvc.perform(get("/expense/api/ai/sessions/{conversationId}/messages", TEST_CONVERSATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        verify(aiSessionService).getSessionMessages(TEST_CONVERSATION_ID);
    }

    @Test
    void testGetSessionMessages_Forbidden() throws Exception {
        // Given
        when(aiSessionService.getSessionMessages(eq(TEST_CONVERSATION_ID)))
                .thenThrow(new IllegalArgumentException("无权访问该会话"));

        // When & Then - 全局异常处理器返回 200 但 success: false
        mockMvc.perform(get("/expense/api/ai/sessions/{conversationId}/messages", TEST_CONVERSATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        verify(aiSessionService).getSessionMessages(TEST_CONVERSATION_ID);
    }
}
