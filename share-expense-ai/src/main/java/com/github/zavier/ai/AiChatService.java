package com.github.zavier.ai;

import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import com.github.zavier.ai.dto.SuggestionsResponse;

public interface AiChatService {
    AiChatResponse chat(AiChatRequest request);
    SuggestionsResponse getSuggestions(String conversationId);
}
