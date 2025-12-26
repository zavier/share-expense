package com.github.zavier.ai.dto;

import lombok.Data;

@Data
public class AiChatResponse {
    private String conversationId;
    private String reply;
    private PendingAction pendingAction;
}
