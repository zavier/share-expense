package com.github.zavier.ai;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Resource
    private AiChatService aiChatService;

    @PostMapping("/chat")
    public SingleResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiChatService.chat(request);
        return SingleResponse.of(response);
    }

    @PostMapping("/confirm")
    public SingleResponse<AiChatResponse> confirm(@Valid @RequestBody ConfirmRequest request) {
        AiChatResponse response = aiChatService.confirm(request.getConversationId(), request.getActionId());
        return SingleResponse.of(response);
    }

    @PostMapping("/cancel")
    public SingleResponse<AiChatResponse> cancel(@Valid @RequestBody CancelRequest request) {
        AiChatResponse response = aiChatService.cancel(request.getConversationId());
        return SingleResponse.of(response);
    }

    @lombok.Data
    public static class ConfirmRequest {
        @NotBlank(message = "会话ID不能为空")
        private String conversationId;

        @NotBlank(message = "操作ID不能为空")
        private String actionId;
    }

    @lombok.Data
    public static class CancelRequest {
        @NotBlank(message = "会话ID不能为空")
        private String conversationId;
    }
}
