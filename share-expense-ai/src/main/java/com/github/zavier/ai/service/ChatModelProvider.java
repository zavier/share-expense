package com.github.zavier.ai.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatModelProvider {

    @Resource
    private OpenAiChatModel openAiChatModel;

    public ChatModel selectChatModel() {
        return openAiChatModel;
    }
}
