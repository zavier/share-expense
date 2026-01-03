package com.github.zavier.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zavier.ai.concurrent.LockContext;
import com.github.zavier.ai.concurrent.LockManager;
import com.github.zavier.ai.entity.AiSessionEntity;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.repository.AiSessionRepository;
import com.github.zavier.ai.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

/**
 * CachedSuggestionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CachedSuggestionServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private AiSessionRepository sessionRepository;

    @Mock
    private SuggestionGenerator suggestionGenerator;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LockManager lockManager;

    @Mock
    private LockContext lockContext;

    @InjectMocks
    private CachedSuggestionService cachedSuggestionService;

    private static final String CONVERSATION_ID = "test-conversation-123";

    @BeforeEach
    void setUp() throws Exception {
        // 重置模拟对象
        reset(conversationRepository, sessionRepository, suggestionGenerator, objectMapper, lockManager);

        // 默认的 lock 行为
        when(lockManager.acquireLock(anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(lockContext);
        when(lockContext.isAcquired()).thenReturn(true);
        doNothing().when(lockContext).close();

        // 默认的 ObjectMapper 行为
        when(objectMapper.writeValueAsString(any()))
            .thenReturn("[{\"text\":\"suggestion\",\"icon\":null,\"priority\":1.0}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
            .thenReturn(List.of(new SuggestionGenerator.SuggestionItem("suggestion", null, 1.0)));
    }

    @Test
    void testGetSuggestionsSync_NoConversationId_ReturnsDefaultSuggestions() {
        // When
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync(null);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(4, result.size());
        verify(conversationRepository, never()).findByConversationIdOrderByCreatedAtDesc(anyString(), any());
        verify(suggestionGenerator, never()).generate(any(), any());
    }

    @Test
    void testGetSuggestionsSync_EmptyConversationId_ReturnsDefaultSuggestions() {
        // When
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync("");

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(4, result.size());
    }

    @Test
    void testGetSuggestionsSync_ValidCacheInSession_ReturnsCachedSuggestions() throws Exception {
        // Given
        List<SuggestionGenerator.SuggestionItem> cachedItems =
            List.of(new SuggestionGenerator.SuggestionItem("建议1", null, 1.0));

        when(sessionRepository.findByConversationId(CONVERSATION_ID))
            .thenReturn(Optional.of(AiSessionEntity.builder()
                .conversationId(CONVERSATION_ID)
                .lastSuggestions("[{\"text\":\"建议1\",\"icon\":null,\"priority\":1.0}]")
                .suggestionsUpdatedAt(LocalDateTime.now().minusMinutes(2)) // 2分钟前，未过期
                .build()));

        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
            .thenReturn(cachedItems);

        // When
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync(CONVERSATION_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("建议1", result.get(0).text());
        verify(suggestionGenerator, never()).generate(any(), any());
    }

    @Test
    void testGetSuggestionsSync_ExpiredCache_GeneratesNewSuggestions() throws Exception {
        // Given
        List<SuggestionGenerator.SuggestionItem> newItems =
            List.of(new SuggestionGenerator.SuggestionItem("新建议", null, 1.0));

        ConversationEntity conversation = ConversationEntity.builder()
            .conversationId(CONVERSATION_ID)
            .suggestions("[{\"text\":\"旧建议\",\"icon\":null,\"priority\":1.0}]")
            .suggestionsUpdatedAt(LocalDateTime.now().minusMinutes(10)) // 10分钟前，已过期
            .suggestionsGenerating(false)
            .build();

        when(sessionRepository.findByConversationId(CONVERSATION_ID))
            .thenReturn(Optional.empty());

        when(conversationRepository.findByConversationIdOrderByCreatedAtDesc(eq(CONVERSATION_ID), any()))
            .thenReturn(List.of(conversation));

        when(conversationRepository.countByConversationId(CONVERSATION_ID))
            .thenReturn(5L); // 不是新会话

        when(suggestionGenerator.generate(any(), any()))
            .thenReturn(newItems);

        when(conversationRepository.save(any(ConversationEntity.class)))
            .thenReturn(conversation);

        // When
        List<SuggestionGenerator.SuggestionItem> result =
            cachedSuggestionService.getSuggestionsSync(CONVERSATION_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("新建议", result.get(0).text());
        verify(suggestionGenerator, times(1)).generate(any(), eq(CONVERSATION_ID));
        verify(conversationRepository, atLeastOnce()).save(any()); // 标记生成中 + 保存结果
    }

    @Test
    void testClearSuggestionsCache_ClearsOnlySessionPreservesConversation() throws Exception {
        // Given
        AiSessionEntity session = AiSessionEntity.builder()
            .conversationId(CONVERSATION_ID)
            .lastSuggestions("[{\"text\":\"建议\",\"icon\":null,\"priority\":1.0}]")
            .suggestionsUpdatedAt(LocalDateTime.now())
            .suggestionsGenerating(false)
            .build();

        when(sessionRepository.findByConversationId(CONVERSATION_ID))
            .thenReturn(Optional.of(session));

        when(sessionRepository.save(any(AiSessionEntity.class)))
            .thenReturn(session);

        // When
        cachedSuggestionService.clearSuggestionsCache(CONVERSATION_ID);

        // Then - 验证只清除 Session 表缓存
        verify(sessionRepository, times(1)).save(argThat(s ->
            s.getLastSuggestions() == null &&
            s.getSuggestionsUpdatedAt() == null &&
            !s.getSuggestionsGenerating()
        ));

        // 验证不会操作 Conversation 表（保留快照）
        verify(conversationRepository, never()).save(any(ConversationEntity.class));
    }

    @Test
    void testClearSuggestionsCache_NoSessionOrConversation_DoesNotThrowError() {
        // Given
        when(sessionRepository.findByConversationId(CONVERSATION_ID))
            .thenReturn(Optional.empty());

        when(conversationRepository.findByConversationIdOrderByCreatedAtDesc(eq(CONVERSATION_ID), any()))
            .thenReturn(List.of());

        // When & Then - 不应抛出异常
        assertDoesNotThrow(() ->
            cachedSuggestionService.clearSuggestionsCache(CONVERSATION_ID)
        );

        verify(sessionRepository, never()).save(any());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void testSaveSuggestionsToDatabase_SavesToSessionAndConversationSnapshot() throws Exception {
        // Given
        List<SuggestionGenerator.SuggestionItem> suggestions =
            List.of(new SuggestionGenerator.SuggestionItem("测试建议", null, 1.0));

        AiSessionEntity session = AiSessionEntity.builder()
            .id(1L)
            .conversationId(CONVERSATION_ID)
            .build();

        ConversationEntity conversation = ConversationEntity.builder()
            .id(1L)
            .conversationId(CONVERSATION_ID)
            .build();

        when(sessionRepository.findByConversationId(CONVERSATION_ID))
            .thenReturn(Optional.of(session));

        when(conversationRepository.findByConversationIdOrderByCreatedAtDesc(eq(CONVERSATION_ID), any()))
            .thenReturn(List.of(conversation));

        when(sessionRepository.save(any(AiSessionEntity.class)))
            .thenReturn(session);

        when(conversationRepository.save(any(ConversationEntity.class)))
            .thenReturn(conversation);

        // When
        cachedSuggestionService.saveSuggestionsToDatabase(CONVERSATION_ID, suggestions);

        // Then - 验证 Session 表保存（活跃缓存）
        ArgumentCaptor<AiSessionEntity> sessionCaptor = ArgumentCaptor.forClass(AiSessionEntity.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        AiSessionEntity savedSession = sessionCaptor.getValue();
        assertNotNull(savedSession.getLastSuggestions());
        assertNotNull(savedSession.getSuggestionsUpdatedAt());
        assertFalse(savedSession.getSuggestionsGenerating());

        // 验证 Conversation 表保存快照（注意：不设置 suggestionsGenerating）
        ArgumentCaptor<ConversationEntity> conversationCaptor = ArgumentCaptor.forClass(ConversationEntity.class);
        verify(conversationRepository).save(conversationCaptor.capture());

        ConversationEntity savedConversation = conversationCaptor.getValue();
        assertNotNull(savedConversation.getSuggestions());
        assertNotNull(savedConversation.getSuggestionsUpdatedAt());
        // Conversation 表快照不需要 suggestionsGenerating 字段
    }
}
