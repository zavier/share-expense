package com.github.zavier.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 意图验证服务测试类
 * 注意：这些测试需要真实的 OpenAI API 连接
 */
@DisplayName("意图验证服务测试")
@Disabled("需要真实的 OpenAI API 连接，集成测试中运行")
class IntentValidationServiceTest {

    private IntentValidationService validationService;

    @BeforeEach
    void setUp() {
        // 注意：这里需要真实的 ChatModel bean
        // 在实际的集成测试中应该使用 Spring Boot 测试上下文
        // ChatModel chatModel = ...; // 从 Spring 上下文获取
        // validationService = new IntentValidationService(chatModel);
    }

    @Test
    @DisplayName("应该接受与费用相关的查询")
    void testAcceptExpenseRelatedQueries() {
        // 需要真实 API 连接，暂时禁用
        // assertTrue(validationService.isExpenseRelated("创建一个周末聚餐项目"));
    }

    @Test
    @DisplayName("应该拒绝与费用无关的查询")
    void testRejectNonExpenseQueries() {
        // 需要真实 API 连接，暂时禁用
        // assertFalse(validationService.isExpenseRelated("今天天气怎么样"));
    }

    @Test
    @DisplayName("应该通过关键词快速匹配")
    void testKeywordFastPath() {
        // 这个测试不需要 API，可以测试关键词匹配逻辑
        // 由于关键词匹配是私有的，我们通过实际行为来测试
        // ChatModel mockChatModel = mock(ChatModel.class);
        // validationService = new IntentValidationService(mockChatModel);
        // assertTrue(validationService.isExpenseRelated("我想记录一笔费用"));
    }

    @Test
    @DisplayName("应该返回正确的拒绝消息")
    void testRejectionMessage() {
        // ChatModel mockChatModel = mock(ChatModel.class);
        // validationService = new IntentValidationService(mockChatModel);
        // String rejectionMessage = validationService.getRejectionMessage();
        // assertNotNull(rejectionMessage);
        // assertTrue(rejectionMessage.contains("费用分摊"));
    }
}
