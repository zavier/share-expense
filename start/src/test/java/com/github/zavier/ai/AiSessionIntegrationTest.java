package com.github.zavier.ai;

import com.github.zavier.Application;
import com.github.zavier.ai.dto.MessageDto;
import com.github.zavier.ai.dto.SessionDto;
import com.github.zavier.ai.entity.AiSessionEntity;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.repository.AiSessionRepository;
import com.github.zavier.ai.repository.ConversationRepository;
import com.github.zavier.domain.user.User;
import com.github.zavier.web.filter.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * AI 会话管理集成测试
 * 测试完整的会话生命周期流程，使用真实数据库
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Rollback
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AiSessionIntegrationTest {

    @Autowired
    private com.github.zavier.ai.AiSessionService aiSessionService;

    @Autowired
    private AiSessionRepository sessionRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    private MockedStatic<UserHolder> mockedUserHolder;

    private static final Integer TEST_USER_ID = 100;
    private static final String TEST_TITLE = "集成测试会话";

    @BeforeEach
    void setUp() {
        // 清理测试数据
        conversationRepository.deleteAll();
        sessionRepository.deleteAll();

        // Mock UserHolder.getUser() 返回测试用户
        User mockUser = mock(User.class);
        org.mockito.Mockito.when(mockUser.getUserId()).thenReturn(TEST_USER_ID);
        mockedUserHolder = mockStatic(UserHolder.class);
        mockedUserHolder.when(UserHolder::getUser).thenReturn(mockUser);
    }

    @AfterEach
    void tearDown() {
        if (mockedUserHolder != null) {
            mockedUserHolder.close();
        }
    }

    // ========== 会话完整生命周期测试 ==========

    @Test
    void testCompleteSessionLifecycle() {
        // 1. 创建会话
        String conversationId = aiSessionService.createSession(TEST_TITLE);
        assertNotNull(conversationId);

        // 验证会话已保存到数据库
        List<AiSessionEntity> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
        assertEquals(1, sessions.size());
        assertEquals(TEST_TITLE, sessions.get(0).getTitle());

        // 2. 添加消息
        ConversationEntity userMessage = ConversationEntity.builder()
                .conversationId(conversationId)
                .userId(TEST_USER_ID)
                .role("user")
                .content("你好，请帮我创建一个项目")
                .createdAt(LocalDateTime.now())
                .build();

        ConversationEntity assistantMessage = ConversationEntity.builder()
                .conversationId(conversationId)
                .userId(TEST_USER_ID)
                .role("assistant")
                .content("好的，请提供项目名称和成员信息")
                .createdAt(LocalDateTime.now().plusSeconds(1))
                .build();

        conversationRepository.save(userMessage);
        conversationRepository.save(assistantMessage);

        // 3. 查询会话列表
        List<SessionDto> sessionList = aiSessionService.listSessions();
        assertEquals(1, sessionList.size());
        assertEquals(TEST_TITLE, sessionList.get(0).title());

        // 4. 查询会话消息
        List<MessageDto> messages = aiSessionService.getSessionMessages(conversationId);
        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).role());
        assertEquals("你好，请帮我创建一个项目", messages.get(0).content());
        assertEquals("assistant", messages.get(1).role());
        assertEquals("好的，请提供项目名称和成员信息", messages.get(1).content());

        // 5. 重命名会话
        String newTitle = "重命名后的会话";
        aiSessionService.renameSession(conversationId, newTitle);

        AiSessionEntity renamedSession = sessionRepository.findByConversationId(conversationId).orElseThrow();
        assertEquals(newTitle, renamedSession.getTitle());

        // 6. 删除会话
        aiSessionService.deleteSession(conversationId);

        // 验证会话和消息都被删除
        assertTrue(sessionRepository.findByConversationId(conversationId).isEmpty());
        assertEquals(0, conversationRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).size());
    }

    // ========== 多用户隔离测试 ==========

    @Test
    void testMultiUserIsolation() {
        // 用户 1 创建会话
        User user1 = mock(User.class);
        org.mockito.Mockito.when(user1.getUserId()).thenReturn(1);
        mockedUserHolder.when(UserHolder::getUser).thenReturn(user1);

        String conversationId1 = aiSessionService.createSession("用户1的会话");

        // 用户 2 创建会话
        User user2 = mock(User.class);
        org.mockito.Mockito.when(user2.getUserId()).thenReturn(2);
        mockedUserHolder.when(UserHolder::getUser).thenReturn(user2);

        String conversationId2 = aiSessionService.createSession("用户2的会话");

        // 用户 1 只能看到自己的会话
        mockedUserHolder.when(UserHolder::getUser).thenReturn(user1);
        List<SessionDto> user1Sessions = aiSessionService.listSessions();
        assertEquals(1, user1Sessions.size());
        assertEquals("用户1的会话", user1Sessions.get(0).title());

        // 用户 2 只能看到自己的会话
        mockedUserHolder.when(UserHolder::getUser).thenReturn(user2);
        List<SessionDto> user2Sessions = aiSessionService.listSessions();
        assertEquals(1, user2Sessions.size());
        assertEquals("用户2的会话", user2Sessions.get(0).title());

        // 用户 1 尝试访问用户 2 的会话应该失败
        mockedUserHolder.when(UserHolder::getUser).thenReturn(user1);
        assertThrows(IllegalArgumentException.class, () -> {
            aiSessionService.getSessionMessages(conversationId2);
        });

        // 用户 2 尝试删除用户 1 的会话应该失败
        mockedUserHolder.when(UserHolder::getUser).thenReturn(user2);
        assertThrows(IllegalArgumentException.class, () -> {
            aiSessionService.deleteSession(conversationId1);
        });
    }

    // ========== 延迟创建会话测试 ==========

    @Test
    void testDelayedSessionCreation() {
        // 1. 创建 conversationId（不保存会话元数据）
        String conversationId = aiSessionService.createSession();

        // 验证此时会话未保存到数据库
        assertTrue(sessionRepository.findByConversationId(conversationId).isEmpty());

        // 2. 模拟用户发送第一条消息，触发延迟创建
        String firstMessage = "这是我的第一条消息，请帮我创建项目";
        ((com.github.zavier.ai.impl.AiSessionServiceImpl) aiSessionService)
                .ensureSessionExists(conversationId, firstMessage);

        // 验证会话已保存
        AiSessionEntity session = sessionRepository.findByConversationId(conversationId).orElseThrow();
        assertEquals(firstMessage, session.getTitle());
        assertEquals(TEST_USER_ID, session.getUserId());

        // 3. 再次调用 ensureSessionExists，不应创建重复会话
        long sessionCount = sessionRepository.count();
        ((com.github.zavier.ai.impl.AiSessionServiceImpl) aiSessionService)
                .ensureSessionExists(conversationId, "另一条消息");

        // 验证没有新增会话
        assertEquals(sessionCount, sessionRepository.count());
    }

    // ========== 会话列表排序测试 ==========

    @Test
    void testSessionListOrdering() {
        // 创建三个会话，不同创建时间
        LocalDateTime now = LocalDateTime.now();

        AiSessionEntity session1 = createAndSaveSession("会话1", now.minusDays(2));
        AiSessionEntity session2 = createAndSaveSession("会话2", now.minusDays(1));
        AiSessionEntity session3 = createAndSaveSession("会话3", now);

        // 查询会话列表，应该按创建时间倒序
        List<SessionDto> sessions = aiSessionService.listSessions();

        assertEquals(3, sessions.size());
        assertEquals("会话3", sessions.get(0).title());
        assertEquals("会话2", sessions.get(1).title());
        assertEquals("会话1", sessions.get(2).title());
    }

    // ========== 会话时间戳更新测试 ==========

    @Test
    void testUpdateSessionTimestamp() {
        // 创建会话
        String conversationId = aiSessionService.createSession(TEST_TITLE);
        AiSessionEntity session = sessionRepository.findByConversationId(conversationId).orElseThrow();

        LocalDateTime originalUpdatedAt = session.getUpdatedAt();

        // 等待一小段时间确保时间戳变化
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 更新时间戳
        ((com.github.zavier.ai.impl.AiSessionServiceImpl) aiSessionService)
                .updateSessionTimestamp(conversationId);

        // 验证时间戳已更新
        AiSessionEntity updatedSession = sessionRepository.findByConversationId(conversationId).orElseThrow();
        assertTrue(updatedSession.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    // ========== 标题生成边界测试 ==========

    @Test
    void testTitleGenerationBoundaryCases() {
        // 短消息
        String shortTitle = ((com.github.zavier.ai.impl.AiSessionServiceImpl) aiSessionService)
                .generateTitleFromMessage("短");
        assertEquals("短", shortTitle);

        // 正好 30 字符
        String exactly30 = "123456789012345678901234567890";
        String title30 = ((com.github.zavier.ai.impl.AiSessionServiceImpl) aiSessionService)
                .generateTitleFromMessage(exactly30);
        assertEquals(30, title30.length());
        assertFalse(title30.endsWith("..."));

        // 31 字符，应该截断
        String over30 = "1234567890123456789012345678901";
        String titleOver30 = ((com.github.zavier.ai.impl.AiSessionServiceImpl) aiSessionService)
                .generateTitleFromMessage(over30);
        assertEquals(33, titleOver30.length()); // 30 + "..."
        assertTrue(titleOver30.endsWith("..."));

        // 空消息
        String emptyTitle = ((com.github.zavier.ai.impl.AiSessionServiceImpl) aiSessionService)
                .generateTitleFromMessage("");
        assertEquals("新对话", emptyTitle);
    }

    // ========== 消息排序测试 ==========

    @Test
    void testMessageOrdering() {
        String conversationId = aiSessionService.createSession(TEST_TITLE);

        // 添加多条消息，时间乱序
        LocalDateTime now = LocalDateTime.now();
        conversationRepository.save(ConversationEntity.builder()
                .conversationId(conversationId)
                .userId(TEST_USER_ID)
                .role("user")
                .content("消息2")
                .createdAt(now.plusMinutes(2))
                .build());

        conversationRepository.save(ConversationEntity.builder()
                .conversationId(conversationId)
                .userId(TEST_USER_ID)
                .role("assistant")
                .content("回复1")
                .createdAt(now.plusMinutes(1))
                .build());

        conversationRepository.save(ConversationEntity.builder()
                .conversationId(conversationId)
                .userId(TEST_USER_ID)
                .role("user")
                .content("消息1")
                .createdAt(now)
                .build());

        // 查询消息，应该按时间正序
        List<MessageDto> messages = aiSessionService.getSessionMessages(conversationId);

        assertEquals(3, messages.size());
        assertEquals("消息1", messages.get(0).content());
        assertEquals("回复1", messages.get(1).content());
        assertEquals("消息2", messages.get(2).content());
    }

    // ========== 边界情况测试 ==========

    @Test
    void testDeleteSessionWithNoMessages() {
        // 创建没有消息的会话
        String conversationId = aiSessionService.createSession(TEST_TITLE);

        // 删除应该成功
        assertDoesNotThrow(() -> aiSessionService.deleteSession(conversationId));

        // 验证会话已删除
        assertTrue(sessionRepository.findByConversationId(conversationId).isEmpty());
    }

    @Test
    void testAccessNonExistentSession() {
        // 查询不存在的会话消息
        assertThrows(IllegalArgumentException.class, () -> {
            aiSessionService.getSessionMessages("non-existent-conversation-id");
        });

        // 删除不存在的会话
        assertThrows(IllegalArgumentException.class, () -> {
            aiSessionService.deleteSession("non-existent-conversation-id");
        });

        // 重命名不存在的会话
        assertThrows(IllegalArgumentException.class, () -> {
            aiSessionService.renameSession("non-existent-conversation-id", "新标题");
        });
    }

    // ========== 辅助方法 ==========

    private AiSessionEntity createAndSaveSession(String title, LocalDateTime createdAt) {
        AiSessionEntity session = AiSessionEntity.builder()
                .conversationId(java.util.UUID.randomUUID().toString())
                .userId(TEST_USER_ID)
                .title(title)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        return sessionRepository.save(session);
    }
}
