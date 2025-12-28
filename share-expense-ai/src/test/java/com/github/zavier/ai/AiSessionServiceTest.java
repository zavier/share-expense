package com.github.zavier.ai;

import com.github.zavier.ai.dto.MessageDto;
import com.github.zavier.ai.dto.SessionDto;
import com.github.zavier.ai.entity.AiSessionEntity;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.impl.AiSessionServiceImpl;
import com.github.zavier.ai.repository.AiSessionRepository;
import com.github.zavier.ai.repository.ConversationRepository;
import com.github.zavier.domain.user.User;
import com.github.zavier.web.filter.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AI 会话管理服务单元测试
 * 覆盖会话创建、查询、删除、重命名、权限验证等核心功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiSessionServiceTest {

    @Mock
    private AiSessionRepository sessionRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private AiSessionServiceImpl aiSessionService;

    private MockedStatic<UserHolder> mockedUserHolder;

    private static final Integer TEST_USER_ID = 1;
    private static final String TEST_CONVERSATION_ID = "test-conv-123";
    private static final String TEST_TITLE = "测试会话";

    @BeforeEach
    void setUp() {
        // Mock UserHolder.getUser() 返回测试用户
        User mockUser = mock(User.class);
        when(mockUser.getUserId()).thenReturn(TEST_USER_ID);
        mockedUserHolder = mockStatic(UserHolder.class);
        mockedUserHolder.when(UserHolder::getUser).thenReturn(mockUser);
    }

    @AfterEach
    void tearDown() {
        if (mockedUserHolder != null) {
            mockedUserHolder.close();
        }
    }

    // ========== listSessions 测试 ==========

    @Test
    void testListSessions_Success() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        AiSessionEntity session1 = AiSessionEntity.builder()
                .id(1L)
                .conversationId("conv-1")
                .userId(TEST_USER_ID)
                .title("会话1")
                .createdAt(now.minusDays(2))
                .updatedAt(now.minusDays(2))
                .build();

        AiSessionEntity session2 = AiSessionEntity.builder()
                .id(2L)
                .conversationId("conv-2")
                .userId(TEST_USER_ID)
                .title("会话2")
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .build();

        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID))
                .thenReturn(List.of(session2, session1)); // 已排序

        // When
        List<SessionDto> result = aiSessionService.listSessions();

        // Then
        assertEquals(2, result.size());
        assertEquals("conv-2", result.get(0).conversationId());
        assertEquals("会话2", result.get(0).title());
        assertEquals("conv-1", result.get(1).conversationId());
        assertEquals("会话1", result.get(1).title());

        verify(sessionRepository).findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
    }

    @Test
    void testListSessions_EmptyList() {
        // Given
        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID))
                .thenReturn(List.of());

        // When
        List<SessionDto> result = aiSessionService.listSessions();

        // Then
        assertTrue(result.isEmpty());
        verify(sessionRepository).findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
    }

    // ========== createSession 测试 ==========

    @Test
    void testCreateSession_ReturnsUuid() {
        // When
        String conversationId = aiSessionService.createSession();

        // Then - 验证返回了 UUID 格式的字符串（延迟创建，不保存到数据库）
        assertNotNull(conversationId);
        assertTrue(conversationId.length() > 0);
        // 验证没有调用 repository（因为延迟创建）
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void testCreateSessionWithTitle_SavesToDatabase() {
        // Given
        when(sessionRepository.save(any(AiSessionEntity.class))).thenAnswer(invocation -> {
            AiSessionEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        // When
        String conversationId = aiSessionService.createSession(TEST_TITLE);

        // Then - createSession 返回的是 conversationId (UUID)，不是 title
        assertNotNull(conversationId);
        verify(sessionRepository).save(argThat(entity ->
                entity.getConversationId() != null &&
                        entity.getUserId().equals(TEST_USER_ID) &&
                        entity.getTitle().equals(TEST_TITLE) &&
                        entity.getCreatedAt() != null &&
                        entity.getUpdatedAt() != null
        ));
    }

    // ========== deleteSession 测试 ==========

    @Test
    void testDeleteSession_Success() {
        // Given
        AiSessionEntity session = AiSessionEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(TEST_USER_ID)
                .title(TEST_TITLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ConversationEntity message1 = ConversationEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(TEST_USER_ID)
                .role("user")
                .content("消息1")
                .createdAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(session));
        when(conversationRepository.findByConversationIdOrderByCreatedAtAsc(TEST_CONVERSATION_ID))
                .thenReturn(List.of(message1));
        doNothing().when(sessionRepository).deleteByConversationId(TEST_CONVERSATION_ID);
        doNothing().when(conversationRepository).deleteAll(anyList());

        // When
        aiSessionService.deleteSession(TEST_CONVERSATION_ID);

        // Then
        verify(sessionRepository).findByConversationId(TEST_CONVERSATION_ID);
        verify(sessionRepository).deleteByConversationId(TEST_CONVERSATION_ID);
        verify(conversationRepository).deleteAll(List.of(message1));
    }

    @Test
    void testDeleteSession_SessionNotFound() {
        // Given
        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiSessionService.deleteSession(TEST_CONVERSATION_ID)
        );
        assertEquals("会话不存在", exception.getMessage());
    }

    @Test
    void testDeleteSession_Forbidden_OtherUserSession() {
        // Given
        AiSessionEntity session = AiSessionEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(999) // 不同用户
                .title(TEST_TITLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(session));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiSessionService.deleteSession(TEST_CONVERSATION_ID)
        );
        assertEquals("无权访问该会话", exception.getMessage());
    }

    // ========== renameSession 测试 ==========

    @Test
    void testRenameSession_Success() {
        // Given
        String newTitle = "新标题";
        AiSessionEntity session = AiSessionEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(TEST_USER_ID)
                .title(TEST_TITLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AiSessionEntity.class))).thenReturn(session);

        // When
        aiSessionService.renameSession(TEST_CONVERSATION_ID, newTitle);

        // Then
        verify(sessionRepository).save(argThat(entity ->
                entity.getTitle().equals(newTitle) &&
                        entity.getUpdatedAt() != null
        ));
    }

    @Test
    void testRenameSession_SessionNotFound() {
        // Given
        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiSessionService.renameSession(TEST_CONVERSATION_ID, "新标题")
        );
        assertEquals("会话不存在", exception.getMessage());
    }

    @Test
    void testRenameSession_Forbidden_OtherUserSession() {
        // Given
        AiSessionEntity session = AiSessionEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(999) // 不同用户
                .title(TEST_TITLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(session));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiSessionService.renameSession(TEST_CONVERSATION_ID, "新标题")
        );
        assertEquals("无权访问该会话", exception.getMessage());
    }

    // ========== getSessionMessages 测试 ==========

    @Test
    void testGetSessionMessages_Success() {
        // Given
        AiSessionEntity session = AiSessionEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(TEST_USER_ID)
                .title(TEST_TITLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ConversationEntity message1 = ConversationEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(TEST_USER_ID)
                .role("user")
                .content("你好")
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();

        ConversationEntity message2 = ConversationEntity.builder()
                .id(2L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(TEST_USER_ID)
                .role("assistant")
                .content("你好，有什么可以帮助你？")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(session));
        when(conversationRepository.findByConversationIdOrderByCreatedAtAsc(TEST_CONVERSATION_ID))
                .thenReturn(List.of(message1, message2));

        // When
        List<MessageDto> result = aiSessionService.getSessionMessages(TEST_CONVERSATION_ID);

        // Then
        assertEquals(2, result.size());
        assertEquals("user", result.get(0).role());
        assertEquals("你好", result.get(0).content());
        assertEquals("assistant", result.get(1).role());
        assertEquals("你好，有什么可以帮助你？", result.get(1).content());
    }

    @Test
    void testGetSessionMessages_SessionNotFound() {
        // Given
        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiSessionService.getSessionMessages(TEST_CONVERSATION_ID)
        );
        assertEquals("会话不存在", exception.getMessage());
    }

    @Test
    void testGetSessionMessages_Forbidden_OtherUserSession() {
        // Given
        AiSessionEntity session = AiSessionEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(999) // 不同用户
                .title(TEST_TITLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(session));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiSessionService.getSessionMessages(TEST_CONVERSATION_ID)
        );
        assertEquals("无权访问该会话", exception.getMessage());
    }

    // ========== generateTitleFromMessage 测试 ==========

    @Test
    void testGenerateTitleFromMessage_ShortMessage() {
        // Given
        String shortMessage = "你好";

        // When
        String result = aiSessionService.generateTitleFromMessage(shortMessage);

        // Then
        assertEquals("你好", result);
    }

    @Test
    void testGenerateTitleFromMessage_LongMessage() {
        // Given - 使用明确的长度计算
        String longMessage = "12345678901234567890123456789012345"; // 35 个字符

        // When
        String result = aiSessionService.generateTitleFromMessage(longMessage);

        // Then - 应该被截断为 30 个字符 + "..." = 33 个字符
        assertEquals(33, result.length()); // 30 + "..."
        assertTrue(result.endsWith("..."));
        assertEquals("123456789012345678901234567890...", result);
    }

    @Test
    void testGenerateTitleFromMessage_EmptyMessage() {
        // Given
        String emptyMessage = "";

        // When
        String result = aiSessionService.generateTitleFromMessage(emptyMessage);

        // Then
        assertEquals("新对话", result);
    }

    @Test
    void testGenerateTitleFromMessage_NullMessage() {
        // When
        String result = aiSessionService.generateTitleFromMessage(null);

        // Then
        assertEquals("新对话", result);
    }

    @Test
    void testGenerateTitleFromMessage_MessageWithSpaces() {
        // Given
        String message = "   消息带空格   ";

        // When
        String result = aiSessionService.generateTitleFromMessage(message);

        // Then
        assertEquals("消息带空格", result);
    }

    // ========== ensureSessionExists 测试 ==========

    @Test
    void testEnsureSessionExists_SessionAlreadyExists() {
        // Given
        AiSessionEntity existingSession = AiSessionEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(TEST_USER_ID)
                .title("已存在")
                .build();

        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(existingSession));

        // When
        aiSessionService.ensureSessionExists(TEST_CONVERSATION_ID, "第一条消息");

        // Then
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void testEnsureSessionExists_SessionNotExists_CreateNew() {
        // Given
        String firstMessage = "这是第一条消息";
        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(AiSessionEntity.class))).thenAnswer(invocation -> {
            AiSessionEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        // When
        aiSessionService.ensureSessionExists(TEST_CONVERSATION_ID, firstMessage);

        // Then
        verify(sessionRepository).save(argThat(entity ->
                entity.getConversationId().equals(TEST_CONVERSATION_ID) &&
                        entity.getUserId().equals(TEST_USER_ID) &&
                        entity.getTitle().equals(firstMessage) &&
                        entity.getCreatedAt() != null &&
                        entity.getUpdatedAt() != null
        ));
    }

    // ========== updateSessionTimestamp 测试 ==========

    @Test
    void testUpdateSessionTimestamp_Success() {
        // Given
        AiSessionEntity session = AiSessionEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(TEST_USER_ID)
                .title(TEST_TITLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AiSessionEntity.class))).thenReturn(session);

        // When
        aiSessionService.updateSessionTimestamp(TEST_CONVERSATION_ID);

        // Then
        verify(sessionRepository).save(argThat(entity ->
                entity.getUpdatedAt() != null
        ));
    }

    @Test
    void testUpdateSessionTimestamp_SessionNotFound() {
        // Given
        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.empty());

        // When
        aiSessionService.updateSessionTimestamp(TEST_CONVERSATION_ID);

        // Then - 不应该抛出异常，只是静默失败
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void testUpdateSessionTimestamp_OtherUserSession_NoUpdate() {
        // Given
        AiSessionEntity session = AiSessionEntity.builder()
                .id(1L)
                .conversationId(TEST_CONVERSATION_ID)
                .userId(999) // 不同用户
                .title(TEST_TITLE)
                .build();

        when(sessionRepository.findByConversationId(TEST_CONVERSATION_ID))
                .thenReturn(Optional.of(session));

        // When
        aiSessionService.updateSessionTimestamp(TEST_CONVERSATION_ID);

        // Then - 不应该更新
        verify(sessionRepository, never()).save(any());
    }
}
