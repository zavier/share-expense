package com.github.zavier.ai;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.ai.dto.MessageDto;
import com.github.zavier.ai.dto.RenameSessionRequest;
import com.github.zavier.ai.dto.SessionDto;
import com.github.zavier.ai.dto.SessionListResponse;
import com.github.zavier.ai.dto.SessionMessagesResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 会话管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/expense/api/ai/sessions")
public class AiSessionController {

    @Resource
    private AiSessionService aiSessionService;

    /**
     * 获取会话列表
     */
    @GetMapping
    public SingleResponse<SessionListResponse> listSessions() {
        List<SessionDto> sessions = aiSessionService.listSessions();
        return SingleResponse.of(new SessionListResponse(sessions));
    }

    /**
     * 创建新会话
     */
    @PostMapping
    public SingleResponse<SessionDto> createSession() {
        String conversationId = aiSessionService.createSession();
        SessionDto session = new SessionDto(null, conversationId, "新对话", null, null);
        return SingleResponse.of(session);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{conversationId}")
    public SingleResponse<Void> deleteSession(@PathVariable("conversationId") String conversationId) {
        aiSessionService.deleteSession(conversationId);
        return SingleResponse.buildSuccess();
    }

    /**
     * 重命名会话
     */
    @PutMapping("/{conversationId}/rename")
    public SingleResponse<Void> renameSession(
            @PathVariable("conversationId") String conversationId,
            @Valid @RequestBody RenameSessionRequest request) {
        aiSessionService.renameSession(conversationId, request.title());
        return SingleResponse.buildSuccess();
    }

    /**
     * 获取会话历史消息
     */
    @GetMapping("/{conversationId}/messages")
    public SingleResponse<SessionMessagesResponse> getSessionMessages(@PathVariable("conversationId") String conversationId) {
        List<MessageDto> messages = aiSessionService.getSessionMessages(conversationId);
        return SingleResponse.of(new SessionMessagesResponse(messages));
    }
}
