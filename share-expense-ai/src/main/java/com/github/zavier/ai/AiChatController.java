package com.github.zavier.ai;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
}
