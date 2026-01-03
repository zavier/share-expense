package com.github.zavier.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zavier.ai.entity.AiSessionEntity;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.repository.AiSessionRepository;
import com.github.zavier.ai.repository.ConversationRepository;
import com.github.zavier.ai.service.CachedSuggestionService;
import com.github.zavier.ai.service.SuggestionGenerator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * CachedSuggestionService 集成测试
 * 测试真实的数据库交互和并发场景
 */
@ActiveProfiles("test")
@SpringBootTest(classes = com.github.zavier.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
class CachedSuggestionServiceIntegrationTest {

    @Autowired
    private CachedSuggestionService cachedSuggestionService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private AiSessionRepository sessionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private SuggestionGenerator suggestionGenerator;

    private String conversationId;
    private static final Integer USER_ID = 1;

    @BeforeEach
    void setUp() {
        // 为每个测试生成唯一的 conversationId
        conversationId = "test-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);

        // Mock SuggestionGenerator 返回默认建议
        when(suggestionGenerator.generate(any(), anyString()))
            .thenReturn(List.of(
                new SuggestionGenerator.SuggestionItem("测试建议1", "原因1", 1.0),
                new SuggestionGenerator.SuggestionItem("测试建议2", "原因2", 0.9)
            ));
    }

    @Test
    @Transactional
    void testGetSuggestionsSync_NewConversation_Generates() {
        // When - 首次请求建议
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync(conversationId);

        // Then
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("创建项目「周末聚餐」，成员有小明、小红、小李", result.get(0).text());
    }

    @Test
    @Transactional
    void testGetSuggestionsSync_CachedSuggestions_ReturnsFromCache() {
        // Given - 创建会话并保存建议
        AiSessionEntity session = AiSessionEntity.builder()
            .conversationId(conversationId)
            .userId(USER_ID)
            .title("测试会话")
            .lastSuggestions("[{\"text\":\"缓存的建议\",\"reason\":null,\"score\":1.0}]")
            .suggestionsUpdatedAt(LocalDateTime.now().minusMinutes(2))
            .suggestionsGenerating(false)
            .build();
        sessionRepository.save(session);

        // When - 请求建议
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync(conversationId);

        // Then - 应该返回缓存建议，不调用 SuggestionGenerator
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("缓存的建议", result.get(0).text());
    }

    @Test
    @Transactional
    void testGetSuggestionsSync_ExpiredCache_Regenerates() {
        // Given - 创建有过期缓存的会话（6分钟前）
        AiSessionEntity session = AiSessionEntity.builder()
            .conversationId(conversationId)
            .userId(USER_ID)
            .title("测试会话")
            .lastSuggestions("[{\"text\":\"过期的建议\",\"reason\":null,\"score\":1.0}]")
            .suggestionsUpdatedAt(LocalDateTime.now().minusMinutes(6))
            .suggestionsGenerating(false)
            .build();
        sessionRepository.save(session);

        // 创建对话记录，使 generateSuggestionsSync 能够正常工作
        ConversationEntity conversation = ConversationEntity.builder()
            .conversationId(conversationId)
            .userId(USER_ID)
            .role("user")
            .content("测试消息")
            .createdAt(LocalDateTime.now())
            .build();
        conversationRepository.save(conversation);

        // When - 请求建议
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync(conversationId);

        // Then - 应该生成新建议
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("测试建议1", result.get(0).text());

        // 验证缓存已更新
        AiSessionEntity updatedSession = sessionRepository.findByConversationId(conversationId).orElseThrow();
        assertNotNull(updatedSession.getLastSuggestions());
        assertTrue(updatedSession.getSuggestionsUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    @Transactional
    void testClearSuggestionsCache_RemovesCachedData() {
        // Given - 创建带缓存的会话
        AiSessionEntity session = AiSessionEntity.builder()
            .conversationId(conversationId)
            .userId(USER_ID)
            .title("测试会话")
            .lastSuggestions("[{\"text\":\"建议\",\"reason\":null,\"score\":1.0}]")
            .suggestionsUpdatedAt(LocalDateTime.now())
            .suggestionsGenerating(false)
            .build();
        sessionRepository.save(session);

        // When - 清除缓存
        cachedSuggestionService.clearSuggestionsCache(conversationId);

        // Then - 缓存应该被清除
        AiSessionEntity clearedSession = sessionRepository.findByConversationId(conversationId).orElseThrow();
        assertNull(clearedSession.getLastSuggestions());
        assertNull(clearedSession.getSuggestionsUpdatedAt());
        assertFalse(clearedSession.getSuggestionsGenerating());
    }

    @Test
    void testConcurrentSuggestions_OnlyOneGeneration() throws InterruptedException {
        // Given - 创建会话和对话记录但没有缓存（在新事务中提交）
        transactionTemplate.executeWithoutResult(status -> {
            AiSessionEntity session = AiSessionEntity.builder()
                .conversationId(conversationId)
                .userId(USER_ID)
                .title("并发测试会话")
                .build();
            sessionRepository.save(session);

            // 创建对话记录，使 generateSuggestionsSync 能够正常工作
            ConversationEntity conversation = ConversationEntity.builder()
                .conversationId(conversationId)
                .userId(USER_ID)
                .role("user")
                .content("测试消息")
                .createdAt(LocalDateTime.now())
                .build();
            conversationRepository.save(conversation);
        });

        AtomicInteger generationCount = new AtomicInteger(0);
        when(suggestionGenerator.generate(any(), anyString()))
            .thenAnswer(invocation -> {
                generationCount.incrementAndGet();
                // 模拟 AI 生成耗时
                Thread.sleep(500);
                return List.of(
                    new SuggestionGenerator.SuggestionItem("并发建议", null, 1.0)
                );
            });

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        // When - 5个线程同时请求建议
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    cachedSuggestionService.getSuggestionsSync(conversationId);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then - 应该只生成一次
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(1, generationCount.get(), "应该只生成一次建议");

        executor.shutdown();
    }

    @Test
    @Transactional
    void testSaveSuggestionsToDatabase_UpdatesBothTables() {
        // Given - 创建会话和对话
        AiSessionEntity session = AiSessionEntity.builder()
            .conversationId(conversationId)
            .userId(USER_ID)
            .title("测试会话")
            .build();
        sessionRepository.save(session);

        ConversationEntity conversation = ConversationEntity.builder()
            .conversationId(conversationId)
            .userId(USER_ID)
            .role("user")
            .content("测试消息")
            .createdAt(LocalDateTime.now())
            .build();
        conversationRepository.save(conversation);

        List<SuggestionGenerator.SuggestionItem> suggestions =
            List.of(new SuggestionGenerator.SuggestionItem("保存测试建议", null, 1.0));

        // When - 保存建议
        cachedSuggestionService.saveSuggestionsToDatabase(conversationId, suggestions);

        // Then - 验证两个表都已更新
        AiSessionEntity updatedSession = sessionRepository.findByConversationId(conversationId).orElseThrow();
        assertNotNull(updatedSession.getLastSuggestions());
        assertNotNull(updatedSession.getSuggestionsUpdatedAt());
        assertFalse(updatedSession.getSuggestionsGenerating());

        ConversationEntity updatedConversation =
            conversationRepository.findByConversationIdOrderByCreatedAtDesc(conversationId,
                org.springframework.data.domain.Pageable.ofSize(1)).get(0);
        assertNotNull(updatedConversation.getSuggestions());
        assertNotNull(updatedConversation.getSuggestionsUpdatedAt());
        assertFalse(updatedConversation.getSuggestionsGenerating());
    }

    @Test
    void testGetSuggestionsSync_NoConversationId_ReturnsDefaultSuggestions() {
        // When
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync(null);

        // Then
        assertNotNull(result);
        assertEquals(4, result.size()); // 默认有4个建议

        // 验证建议内容
        List<String> texts = result.stream().map(SuggestionGenerator.SuggestionItem::text).toList();
        assertTrue(texts.contains("创建项目「周末聚餐」，成员有小明、小红、小李"));
    }

    @Test
    @Transactional
    void testCacheValidity_Within5Minutes_ReturnsCache() {
        // Given - 2分钟前的缓存
        AiSessionEntity session = AiSessionEntity.builder()
            .conversationId(conversationId)
            .userId(USER_ID)
            .title("测试会话")
            .lastSuggestions("[{\"text\":\"2分钟前的建议\",\"reason\":null,\"score\":1.0}]")
            .suggestionsUpdatedAt(LocalDateTime.now().minusMinutes(2))
            .suggestionsGenerating(false)
            .build();
        sessionRepository.save(session);

        // When
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync(conversationId);

        // Then - 应该返回缓存
        assertEquals(1, result.size());
        assertEquals("2分钟前的建议", result.get(0).text());
    }

    @Test
    @Transactional
    void testCacheValidity_Exactly5Minutes_ReturnsCache() {
        // Given - 正好5分钟前的缓存
        AiSessionEntity session = AiSessionEntity.builder()
            .conversationId(conversationId)
            .userId(USER_ID)
            .title("测试会话")
            .lastSuggestions("[{\"text\":\"5分钟前的建议\",\"reason\":null,\"score\":1.0}]")
            .suggestionsUpdatedAt(LocalDateTime.now().minusMinutes(5))
            .suggestionsGenerating(false)
            .build();
        sessionRepository.save(session);

        // When
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync(conversationId);

        // Then - 5分钟整应该仍然有效
        assertEquals(1, result.size());
        assertEquals("5分钟前的建议", result.get(0).text());
    }

    @Test
    @Transactional
    void testCacheValidity_Over5Minutes_Regenerates() {
        // Given - 5分1秒前的缓存（刚过期）
        AiSessionEntity session = AiSessionEntity.builder()
            .conversationId(conversationId)
            .userId(USER_ID)
            .title("测试会话")
            .lastSuggestions("[{\"text\":\"过期的建议\",\"reason\":null,\"score\":1.0}]")
            .suggestionsUpdatedAt(LocalDateTime.now().minusMinutes(5).minusSeconds(1))
            .suggestionsGenerating(false)
            .build();
        sessionRepository.save(session);

        // 创建对话记录，使 generateSuggestionsSync 能够正常工作
        ConversationEntity conversation = ConversationEntity.builder()
            .conversationId(conversationId)
            .userId(USER_ID)
            .role("user")
            .content("测试消息")
            .createdAt(LocalDateTime.now())
            .build();
        conversationRepository.save(conversation);

        // When
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync(conversationId);

        // Then - 应该重新生成
        assertNotEquals("过期的建议", result.get(0).text());
        assertEquals("测试建议1", result.get(0).text());
    }
}
