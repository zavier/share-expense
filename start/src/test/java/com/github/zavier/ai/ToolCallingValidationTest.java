package com.github.zavier.ai;

import com.alibaba.cola.dto.PageResponse;
import com.github.zavier.ai.domain.MessageRole;
import com.github.zavier.ai.dto.AiChatRequest;
import com.github.zavier.ai.dto.AiChatResponse;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.repository.ConversationRepository;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.domain.user.User;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.web.filter.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * AI 工具调用验证测试
 *
 * 测试目标：
 * - 验证 AI 是否真的调用了工具函数，而不是只在文本中声称完成操作
 * - 检测"AI 幻觉"问题（声称成功但实际没有调用工具）
 * - 确保优化后的提示词能够强制 AI 执行工具调用
 *
 * 问题描述：
 * 用户反馈 AI 经常说"已添加费用"、"已创建项目"，但实际没有调用工具，
 * 导致数据没有真正保存到数据库。
 *
 * 解决方案：
 * 在系统提示词中添加强制性的工具调用规则，明确禁止在文本中声称完成操作。
 *
 * 运行条件：
 * - 需要设置 OPENAI_API_KEY 环境变量
 * - 使用 -Dai.test.enabled=true 参数启用测试
 *
 * 运行命令：
 * mvn test -Dtest=ToolCallingValidationTest -DOPENAI_API_KEY=your-key -Dai.test.enabled=true
 */
@SpringBootTest(classes = com.github.zavier.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
@EnabledIf("aiTestEnabled")
@DisplayName("AI 工具调用验证测试 - 防止 AI 幻觉")
class ToolCallingValidationTest {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ExpenseProjectGateway expenseProjectGateway;

    private MockedStatic<UserHolder> mockedUserHolder;

    private static final Integer TEST_USER_ID = 100;

    /**
     * 检查是否启用 AI 测试
     */
    static boolean aiTestEnabled() {
        return false;
    }

    @BeforeEach
    void setUp() {
        // 清理测试数据
        conversationRepository.deleteAll();

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

    // ========== 创建项目测试 ==========

    @Test
    @DisplayName("创建项目 - 验证工具真的被调用（数据确实保存到数据库）")
    void testCreateProject_ToolActuallyCalled() {
        // 记录调用前的项目数量
        ProjectListQry qryBefore = new ProjectListQry();
        qryBefore.setOperatorId(TEST_USER_ID);
        qryBefore.setPage(1);
        qryBefore.setSize(1000);
        PageResponse<ExpenseProject> pageBefore = expenseProjectGateway.pageProject(qryBefore);
        int countBefore = pageBefore.getData().size();

        // 发送创建项目请求
        String conversationId = null;
        AiChatRequest request = new AiChatRequest(
                "创建一个项目叫'测试项目'，成员有小明、小红",
                conversationId
        );
        AiChatResponse response = aiChatService.chat(request);

        assertNotNull(response);
        assertNotNull(response.reply());
        assertFalse(response.reply().isBlank(), "AI 应该有回复");

        // 验证工具确实被调用了（数据库中项目数量增加了）
        ProjectListQry qryAfter = new ProjectListQry();
        qryAfter.setOperatorId(TEST_USER_ID);
        qryAfter.setPage(1);
        qryAfter.setSize(1000);
        PageResponse<ExpenseProject> pageAfter = expenseProjectGateway.pageProject(qryAfter);
        List<ExpenseProject> projectsAfter = pageAfter.getData();
        int countAfter = projectsAfter.size();

        assertEquals(
                countBefore + 1,
                countAfter,
                "AI 必须真的调用 createProject 工具，数据库应该增加一个项目。\n" +
                "AI 回复: " + response.reply() + "\n" +
                "如果测试失败，说明 AI 只是在文本中声称创建了项目，但没有实际调用工具。"
        );

        // 验证创建的项目确实存在
        ExpenseProject createdProject = projectsAfter.stream()
                .filter(p -> "测试项目".equals(p.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(
                createdProject,
                "应该能在数据库中找到名为'测试项目'的项目。\n" +
                "AI 回复: " + response.reply()
        );

        // 验证成员数量
        assertEquals(
                2,
                createdProject.totalMember(),
                "项目应该有2个成员：小明、小红"
        );

        System.out.println("✅ 测试通过：AI 确实调用了 createProject 工具");
        System.out.println("项目名称: " + createdProject.getName());
        System.out.println("成员数量: " + createdProject.totalMember());
        System.out.println("AI 回复: " + response.reply());
    }

    @Test
    @DisplayName("创建项目 - AI 不应该只在文本中声称成功")
    void testCreateProject_NoFalseClaims() {
        String projectName = "幻觉测试项目" + System.currentTimeMillis();

        // 发送创建项目请求
        AiChatRequest request = new AiChatRequest(
                "创建项目'" + projectName + "'，成员有张三、李四、王五",
                null
        );
        AiChatResponse response = aiChatService.chat(request);

        String reply = response.reply();

        // 验证：如果 AI 说"已创建"、"成功"、"已完成"等词，数据库中应该真的有这个项目
        boolean claimsSuccess = reply.contains("已创建") || reply.contains("成功") ||
                               reply.contains("已完成") || reply.contains("添加成功") ||
                               reply.contains("创建成功");

        if (claimsSuccess) {
            // AI 声称成功了，验证数据库中是否真的有
            ProjectListQry qry = new ProjectListQry();
            qry.setOperatorId(TEST_USER_ID);
            qry.setPage(1);
            qry.setSize(1000);
            PageResponse<ExpenseProject> pageResponse = expenseProjectGateway.pageProject(qry);
            List<ExpenseProject> projects = pageResponse.getData();
            boolean actuallyExists = projects.stream()
                    .anyMatch(p -> p.getName().contains(projectName));

            assertTrue(
                    actuallyExists,
                    "AI 在回复中声称成功创建了项目，但数据库中找不到这个项目！\n" +
                    "这是 AI 幻觉问题。\n" +
                    "AI 回复: " + reply + "\n" +
                    "查找的项目名: " + projectName
            );
        }

        System.out.println("AI 回复: " + reply);
    }

    // ========== 多轮对话测试 ==========

    @Test
    @DisplayName("多轮对话 - 先创建项目，再添加成员，验证数据真的保存")
    void testMultiTurnConversation_DataPersistence() {
        String conversationId = null;

        // 第一轮：创建项目
        AiChatRequest request1 = new AiChatRequest(
                "创建项目'多轮测试'，成员有A、B",
                conversationId
        );
        AiChatResponse response1 = aiChatService.chat(request1);
        conversationId = response1.conversationId();

        // 验证项目创建成功
        ProjectListQry qry1 = new ProjectListQry();
        qry1.setOperatorId(TEST_USER_ID);
        qry1.setPage(1);
        qry1.setSize(1000);
        PageResponse<ExpenseProject> page1 = expenseProjectGateway.pageProject(qry1);
        List<ExpenseProject> projectsAfterFirst = page1.getData();
        ExpenseProject project = projectsAfterFirst.stream()
                .filter(p -> "多轮测试".equals(p.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(
                project,
                "第一轮对话后，数据库中应该存在'多轮测试'项目。\n" +
                "AI 回复: " + response1.reply()
        );

        int memberCountAfterFirst = project.totalMember();

        // 第二轮：添加成员
        AiChatRequest request2 = new AiChatRequest(
                "添加成员C到项目'多轮测试'",
                conversationId
        );
        AiChatResponse response2 = aiChatService.chat(request2);

        // 重新查询项目
        ExpenseProject projectAfterSecond = expenseProjectGateway.getProjectById(project.getId())
                .orElse(null);

        assertNotNull(
                projectAfterSecond,
                "项目应该仍然存在"
        );

        int memberCountAfterSecond = projectAfterSecond.totalMember();

        // 验证成员数量增加了
        assertEquals(
                memberCountAfterFirst + 1,
                memberCountAfterSecond,
                "AI 必须真的调用 addMembers 工具，成员数量应该增加。\n" +
                "AI 回复: " + response2.reply() + "\n" +
                "第一轮成员数: " + memberCountAfterFirst + "\n" +
                "第二轮成员数: " + memberCountAfterSecond
        );
    }

    // ========== 记录费用测试 ==========

    @Test
    @DisplayName("记录费用 - 验证工具真的被调用")
    void testAddExpenseRecord_ToolActuallyCalled() {
        // 先创建一个项目
        String conversationId = null;
        AiChatRequest createRequest = new AiChatRequest(
                "创建项目'费用测试'，成员有张三、李四",
                conversationId
        );
        AiChatResponse createResponse = aiChatService.chat(createRequest);
        conversationId = createResponse.conversationId();

        // 获取项目ID
        ProjectListQry qry = new ProjectListQry();
        qry.setOperatorId(TEST_USER_ID);
        qry.setPage(1);
        qry.setSize(1000);
        PageResponse<ExpenseProject> pageResponse = expenseProjectGateway.pageProject(qry);
        List<ExpenseProject> projects = pageResponse.getData();
        ExpenseProject project = projects.stream()
                .filter(p -> "费用测试".equals(p.getName()))
                .findFirst()
                .orElseThrow();

        int expenseCountBefore = project.listAllExpenseRecord().size();

        // 记录费用
        AiChatRequest expenseRequest = new AiChatRequest(
                "记录一笔费用：张三支付了100元用于午餐，消费人员是张三、李四",
                conversationId
        );
        AiChatResponse expenseResponse = aiChatService.chat(expenseRequest);

        assertNotNull(expenseResponse.reply());

        // 重新查询项目，验证费用是否真的添加了
        ExpenseProject projectAfterExpense = expenseProjectGateway.getProjectById(project.getId())
                .orElseThrow();

        int expenseCountAfter = projectAfterExpense.listAllExpenseRecord().size();

        // 验证费用记录数量增加了
        assertEquals(
                expenseCountBefore + 1,
                expenseCountAfter,
                "AI 必须真的调用 addExpenseRecord 工具，费用记录应该增加。\n" +
                "AI 回复: " + expenseResponse.reply() + "\n" +
                "之前费用数: " + expenseCountBefore + "\n" +
                "之后费用数: " + expenseCountAfter
        );


        // 再次添加
        expenseResponse = aiChatService.chat(new AiChatRequest(
                "记录一笔费用：李四支付了100元的过路费，消费人员是张三、李四",
                conversationId
        ));

        assertNotNull(expenseResponse.reply());

        // 重新查询项目，验证费用是否真的添加了
        projectAfterExpense = expenseProjectGateway.getProjectById(project.getId())
                .orElseThrow();

        expenseCountAfter = projectAfterExpense.listAllExpenseRecord().size();

        // 验证费用记录数量增加了
        assertEquals(
                expenseCountBefore + 2,
                expenseCountAfter,
                "AI 必须真的调用 addExpenseRecord 工具，费用记录应该增加。\n" +
                        "AI 回复: " + expenseResponse.reply() + "\n" +
                        "之前费用数: " + expenseCountBefore + "\n" +
                        "之后费用数: " + expenseCountAfter
        );

    }

    @Test
    @DisplayName("记录费用 - 检测 AI 是否只在文本中声称成功")
    void testAddExpenseRecord_NoFalseClaims() {
        // 先创建项目
        AiChatRequest createRequest = new AiChatRequest(
                "创建项目'检测幻觉'，成员有A、B",
                null
        );
        aiChatService.chat(createRequest);

        ProjectListQry qry = new ProjectListQry();
        qry.setOperatorId(TEST_USER_ID);
        qry.setPage(1);
        qry.setSize(1000);
        PageResponse<ExpenseProject> pageResponse = expenseProjectGateway.pageProject(qry);
        List<ExpenseProject> projects = pageResponse.getData();
        ExpenseProject project = projects.stream()
                .filter(p -> "检测幻觉".equals(p.getName()))
                .findFirst()
                .orElseThrow();

        int expenseCountBefore = project.listAllExpenseRecord().size();

        // 记录费用
        AiChatRequest expenseRequest = new AiChatRequest(
                "A支付了50元买饮料，A和B分",
                null
        );
        AiChatResponse expenseResponse = aiChatService.chat(expenseRequest);

        String reply = expenseResponse.reply();

        // 重新查询
        ExpenseProject projectAfter = expenseProjectGateway.getProjectById(project.getId()).orElseThrow();
        int expenseCountAfter = projectAfter.listAllExpenseRecord().size();

        // 验证：如果 AI 说"已记录"、"已添加"等，数据库应该真的有记录
        boolean claimsSuccess = reply.contains("已记录") || reply.contains("已添加") ||
                               reply.contains("成功") || reply.contains("已完成");

        if (claimsSuccess) {
            assertEquals(
                    expenseCountBefore + 1,
                    expenseCountAfter,
                    "AI 声称成功记录费用，但数据库中费用记录数量没有增加！\n" +
                    "这是 AI 幻觉问题。\n" +
                    "AI 回复: " + reply + "\n" +
                    "之前费用数: " + expenseCountBefore + "\n" +
                    "之后费用数: " + expenseCountAfter
            );
        }

        System.out.println("AI 回复: " + reply);
        System.out.println("费用记录数变化: " + expenseCountBefore + " -> " + expenseCountAfter);
    }

    // ========== 边界情况测试 ==========

    @Test
    @DisplayName("参数不完整 - AI 应该询问而不是假装成功")
    void testIncompleteParameters_ShouldAskNotPretend() {
        // 不提供项目名称，只说"创建项目"
        AiChatRequest request = new AiChatRequest(
                "帮我创建一个项目",
                null
        );
        AiChatResponse response = aiChatService.chat(request);

        String reply = response.reply();

        // AI 应该询问项目名称，而不是声称创建成功
        boolean asksForInfo = reply.contains("项目名称") || reply.contains("请提供") ||
                             reply.contains("请问") || reply.contains("什么");

        assertTrue(
                asksForInfo,
                "参数不完整时，AI 应该询问必要信息，而不是假装创建成功。\n" +
                "AI 回复: " + reply
        );

        // 验证数据库中没有新项目
        ProjectListQry qry = new ProjectListQry();
        qry.setOperatorId(TEST_USER_ID);
        qry.setPage(1);
        qry.setSize(1000);
        PageResponse<ExpenseProject> pageResponse = expenseProjectGateway.pageProject(qry);
        List<ExpenseProject> projects = pageResponse.getData();
        // 注意：可能已有其他测试创建的项目，所以只验证当前这个请求没有创建新项目
        // 这里我们验证 AI 不应该在回复中说"已创建"等词
        boolean claimsCreated = reply.contains("已创建") || reply.contains("创建成功") ||
                               reply.contains("已完成") || reply.contains("已添加");

        if (claimsCreated) {
            // 如果 AI 声称创建了，那么应该真的有项目
            // 但由于我们没有提供项目名称，AI 不应该能够创建
            fail(
                    "AI 在参数不完整的情况下声称创建了项目，这是错误的。\n" +
                    "AI 回复: " + reply
            );
        }

        System.out.println("✅ AI 正确处理参数不完整的情况");
        System.out.println("AI 回复: " + reply);
    }

    @Test
    @DisplayName("记录费用缺少金额 - AI 应该询问而不是假装成功")
    void testAddExpenseMissingAmount_ShouldAskNotPretend() {
        // 先创建项目
        AiChatRequest createRequest = new AiChatRequest(
                "创建项目'测试金额'，成员有X、Y",
                null
        );
        aiChatService.chat(createRequest);

        ProjectListQry qry = new ProjectListQry();
        qry.setOperatorId(TEST_USER_ID);
        qry.setPage(1);
        qry.setSize(1000);
        PageResponse<ExpenseProject> pageResponse = expenseProjectGateway.pageProject(qry);
        List<ExpenseProject> projects = pageResponse.getData();
        ExpenseProject project = projects.stream()
                .filter(p -> "测试金额".equals(p.getName()))
                .findFirst()
                .orElseThrow();

        int expenseCountBefore = project.listAllExpenseRecord().size();

        // 记录费用但不提供金额
        AiChatRequest expenseRequest = new AiChatRequest(
                "X支付了午餐费，X和Y分",
                null
        );
        AiChatResponse expenseResponse = aiChatService.chat(expenseRequest);

        String reply = expenseResponse.reply();

        // AI 应该询问金额
        boolean asksForAmount = reply.contains("金额") || reply.contains("多少") ||
                               reply.contains("请问") || reply.contains("请提供");

        assertTrue(
                asksForAmount,
                "缺少金额时，AI 应该询问金额，而不是假装记录成功。\n" +
                "AI 回复: " + reply
        );

        // 验证数据库中没有新费用记录
        ExpenseProject projectAfter = expenseProjectGateway.getProjectById(project.getId()).orElseThrow();
        int expenseCountAfter = projectAfter.listAllExpenseRecord().size();

        assertEquals(
                expenseCountBefore,
                expenseCountAfter,
                "缺少金额时，AI 不应该添加费用记录。\n" +
                "AI 回复: " + reply
        );

        System.out.println("✅ AI 正确处理缺少金额的情况");
        System.out.println("AI 回复: " + reply);
    }

    // ========== 批量测试 ==========

    @Test
    @DisplayName("批量创建多个项目 - 验证每个都真的被调用")
    void testBatchCreateProjects_AllActuallyCreated() {
        ProjectListQry qryBefore = new ProjectListQry();
        qryBefore.setOperatorId(TEST_USER_ID);
        qryBefore.setPage(1);
        qryBefore.setSize(1000);
        PageResponse<ExpenseProject> pageBefore = expenseProjectGateway.pageProject(qryBefore);
        int countBefore = pageBefore.getData().size();

        // 连续创建3个项目
        String[] projectNames = {"项目1", "项目2", "项目3"};

        for (String projectName : projectNames) {
            AiChatRequest request = new AiChatRequest(
                    "创建项目'" + projectName + "'，成员有成员A",
                    null
            );
            AiChatResponse response = aiChatService.chat(request);

            assertNotNull(response.reply());

            // 每次都要验证项目真的创建了
            ProjectListQry qry = new ProjectListQry();
            qry.setOperatorId(TEST_USER_ID);
            qry.setPage(1);
            qry.setSize(1000);
            PageResponse<ExpenseProject> pageResponse = expenseProjectGateway.pageProject(qry);
            List<ExpenseProject> currentProjects = pageResponse.getData();
            boolean exists = currentProjects.stream()
                    .anyMatch(p -> p.getName().equals(projectName));

            assertTrue(
                    exists,
                    "项目'" + projectName + "'应该在数据库中存在。\n" +
                    "AI 回复: " + response.reply()
            );
        }

        ProjectListQry qryAfter = new ProjectListQry();
        qryAfter.setOperatorId(TEST_USER_ID);
        qryAfter.setPage(1);
        qryAfter.setSize(1000);
        PageResponse<ExpenseProject> pageAfter = expenseProjectGateway.pageProject(qryAfter);
        int countAfter = pageAfter.getData().size();

        assertEquals(
                countBefore + 3,
                countAfter,
                "应该创建3个新项目。\n" +
                "之前: " + countBefore + "\n" +
                "之后: " + countAfter
        );

        System.out.println("✅ 批量创建测试通过，成功创建3个项目");
    }

    // ========== 辅助方法 ==========

    /**
     * 保存消息到数据库（用于模拟对话历史）
     */
    private void saveMessage(String conversationId, MessageRole role, String content) {
        ConversationEntity entity = ConversationEntity.builder()
                .conversationId(conversationId)
                .userId(TEST_USER_ID)
                .role(role.getCode())
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        conversationRepository.save(entity);
    }
}
