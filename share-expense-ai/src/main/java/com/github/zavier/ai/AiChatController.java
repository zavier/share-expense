package com.github.zavier.ai;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    @PostMapping("/chat")
    public SingleResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        AiChatResponse response = aiChatService.chat(request);
        return SingleResponse.of(response);
    }

    @PostMapping("/confirm")
    public SingleResponse<AiChatResponse> confirm(@RequestBody ConfirmRequest request) {
        AiChatResponse response = aiChatService.confirm(request.getConversationId(), request.getActionId());
        return SingleResponse.of(response);
    }

    @PostMapping("/cancel")
    public SingleResponse<AiChatResponse> cancel(@RequestBody CancelRequest request) {
        AiChatResponse response = aiChatService.cancel(request.getConversationId());
        return SingleResponse.of(response);
    }

    @lombok.Data
    public static class ConfirmRequest {
        private String conversationId;
        private String actionId;
    }

    @lombok.Data
    public static class CancelRequest {
        private String conversationId;
    }
}
