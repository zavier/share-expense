package com.github.zavier.ai.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatModelProvider {

    private OpenAiChatModel deepseekChatModel;

    private OpenAiChatModel longCatChatModel;

    public ChatModel selectChatModel() {
        return deepseekChatModel;
    }

    public ChatModel selectFastChatModel() {
        return longCatChatModel;
    }

    @PostConstruct
    public void init() {
        initLongCatChatModel();
        initDeepSeekChatModel();
    }

    public void initDeepSeekChatModel() {
        var apiKey = OpenAiApi.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .baseUrl("https://api.deepseek.com")
                .build();
        var chatOptions = OpenAiChatOptions.builder()
                .model("deepseek-chat")
                .temperature(0.4)
                .maxTokens(2000)
                .build();
        this.deepseekChatModel = OpenAiChatModel.builder()
                .openAiApi(apiKey)
                .defaultOptions(chatOptions)
                .build();
    }


    public void initLongCatChatModel() {
        var apiKey = OpenAiApi.builder()
                .apiKey(System.getenv("LONGCAT_API_KEY"))
                .baseUrl("https://api.longcat.chat/openai")
                .build();
        var aiChatOptions = OpenAiChatOptions.builder()
                .model("LongCat-Flash-Chat")
                .temperature(0.7)
                .maxTokens(5000)
                .build();
        this.longCatChatModel = OpenAiChatModel.builder()
                .openAiApi(apiKey)
                .defaultOptions(aiChatOptions)
                .build();
    }
}
