package com.github.zavier.ai;

import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request);
}
