package com.github.zavier.miniprogram;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.domain.utils.ShareTokenHelper;
import com.github.zavier.dto.ExpenseRecordQry;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.ProjectSharingQry;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.dto.wx.ProjectShareDTO;
import com.github.zavier.project.ExpenseApplicationService;
import com.github.zavier.user.UserApplicationService;
import com.github.zavier.vo.SingleResponseVo;
import com.github.zavier.web.filter.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseMiniProgramAdaptorTest {

    @Mock
    private UserApplicationService userApplicationService;

    @Mock
    private ExpenseApplicationService expenseApplicationService;

    @Mock
    private HttpServletResponse httpServletResponse;

    @InjectMocks
    private ExpenseMiniProgramAdaptor adaptor;

    private MockedStatic<ShareTokenHelper> shareTokenHelperMock;
    private MockedStatic<UserHolder> userHolderMock;
    private com.github.zavier.domain.user.User mockUser;

    @BeforeEach
    void setUp() {
        shareTokenHelperMock = mockStatic(ShareTokenHelper.class);
        userHolderMock = mockStatic(UserHolder.class);
        mockUser = new com.github.zavier.domain.user.User();
        mockUser.setUserId(1);
        mockUser.setUserName("testuser");
        userHolderMock.when(UserHolder::getUser).thenReturn(mockUser);
    }

    @AfterEach
    void tearDown() {
        shareTokenHelperMock.close();
        userHolderMock.close();
    }

    // ==================== wxLogin ====================

    @Test
    void wxLogin_blankCode_shouldReturnFailure() {
        SingleResponseVo result = adaptor.wxLogin("", httpServletResponse);

        assertFalse(result.getStatus() == 0);
        assertEquals("WX_LOGIN_FAILED", result.getErrCode());
        verifyNoInteractions(userApplicationService);
    }

    @Test
    void wxLogin_nullCode_shouldReturnFailure() {
        SingleResponseVo result = adaptor.wxLogin(null, httpServletResponse);

        assertFalse(result.getStatus() == 0);
        verifyNoInteractions(userApplicationService);
    }

    @Test
    void wxLogin_validCode_shouldSetCookieAndReturnToken() {
        SingleResponse<String> tokenResp = SingleResponse.of("jwt-token-xyz");
        when(userApplicationService.wxLogin("valid-code")).thenReturn(tokenResp);

        SingleResponseVo result = adaptor.wxLogin("valid-code", httpServletResponse);

        assertTrue(result.getStatus() == 0);
        assertEquals("jwt-token-xyz", result.getData());
        verify(httpServletResponse).addCookie(argThat(cookie ->
                "jwtToken".equals(cookie.getName()) && "/".equals(cookie.getPath())
        ));
    }

    @Test
    void wxLogin_serviceFailure_shouldReturnFailure() {
        SingleResponse<String> tokenResp = SingleResponse.buildFailure("ERR", "微信服务异常");
        when(userApplicationService.wxLogin("bad-code")).thenReturn(tokenResp);

        SingleResponseVo result = adaptor.wxLogin("bad-code", httpServletResponse);

        assertFalse(result.getStatus() == 0);
        verify(httpServletResponse, never()).addCookie(any());
    }

    // ==================== generateProjectShareToken ====================

    @Test
    void generateShareToken_userNotOwner_shouldReturnFailure() {
        mockUser.setUserId(1);
        // pageProject returns empty → not owner
        PageResponse<ProjectDTO> emptyPage = PageResponse.of(
                Collections.emptyList(), 0, 10, 1);
        when(expenseApplicationService.pageProject(any(ProjectListQry.class)))
                .thenReturn(emptyPage);

        SingleResponseVo result = adaptor.generateProjectShareToken(42);

        assertFalse(result.getStatus() == 0);
        assertEquals("PROJECT_NOT_FOUND", result.getErrCode());
    }

    @Test
    void generateShareToken_multipleProjects_shouldReturnFailure() {
        mockUser.setUserId(1);
        ProjectDTO p1 = new ProjectDTO();
        p1.setProjectId(42);
        ProjectDTO p2 = new ProjectDTO();
        p2.setProjectId(99);
        PageResponse<ProjectDTO> page = PageResponse.of(List.of(p1, p2), 2, 10, 1);
        when(expenseApplicationService.pageProject(any(ProjectListQry.class)))
                .thenReturn(page);

        SingleResponseVo result = adaptor.generateProjectShareToken(42);

        assertFalse(result.getStatus() == 0);
        assertEquals("PROJECT_NOT_FOUND", result.getErrCode());
    }

    @Test
    void generateShareToken_validOwner_shouldReturnToken() {
        mockUser.setUserId(1);
        ProjectDTO project = new ProjectDTO();
        project.setProjectId(42);
        PageResponse<ProjectDTO> page = PageResponse.of(List.of(project), 1, 10, 1);
        when(expenseApplicationService.pageProject(any(ProjectListQry.class)))
                .thenReturn(page);
        shareTokenHelperMock.when(() -> ShareTokenHelper.generateShareToken(anyString()))
                .thenReturn("share-token-abc");

        SingleResponseVo result = adaptor.generateProjectShareToken(42);

        assertTrue(result.getStatus() == 0);
        assertEquals("share-token-abc", result.getData());
    }

    // ==================== listShareRecord ====================

    @Test
    void listShareRecord_invalidToken_shouldReturnFailure() {
        shareTokenHelperMock.when(() -> ShareTokenHelper.validateShareToken("bad-token"))
                .thenReturn(false);

        SingleResponseVo<Map<String, List<ExpenseRecordDTO>>> result = adaptor.listShareRecord("bad-token");

        assertFalse(result.getStatus() == 0);
        assertEquals("SHARE_TOKEN_INVALID", result.getErrCode());
    }

    @Test
    void listShareRecord_validToken_shouldReturnRecords() {
        shareTokenHelperMock.when(() -> ShareTokenHelper.validateShareToken("valid-token"))
                .thenReturn(true);

        ProjectShareDTO shareDTO = new ProjectShareDTO();
        shareDTO.setProjectId(42);
        shareDTO.setUserId(1);
        shareTokenHelperMock.when(() -> ShareTokenHelper.getShareTokenBody("valid-token"))
                .thenReturn("{\"projectId\":42,\"userId\":1}");

        ExpenseRecordDTO record = new ExpenseRecordDTO();
        record.setRecordId(1);
        record.setPayMember("Alice");
        record.setAmount(new BigDecimal("100"));
        SingleResponse<List<ExpenseRecordDTO>> recordResp = SingleResponse.of(List.of(record));
        when(expenseApplicationService.listRecord(any(ExpenseRecordQry.class)))
                .thenReturn(recordResp);

        SingleResponseVo<Map<String, List<ExpenseRecordDTO>>> result = adaptor.listShareRecord("valid-token");

        assertTrue(result.getStatus() == 0);
        List<ExpenseRecordDTO> rows = result.getData().get("rows");
        assertEquals(1, rows.size());
        assertEquals("Alice", rows.get(0).getPayMember());
    }

    // ==================== getProjectSharingDetail ====================

    @Test
    void getProjectSharingDetail_invalidToken_shouldReturnFailure() {
        shareTokenHelperMock.when(() -> ShareTokenHelper.validateShareToken("bad-token"))
                .thenReturn(false);

        SingleResponseVo result = adaptor.getProjectSharingDetail("bad-token");

        assertFalse(result.getStatus() == 0);
        assertEquals("SHARE_TOKEN_INVALID", result.getErrCode());
    }

    @Test
    void getProjectSharingDetail_validToken_shouldReturnSharing() {
        shareTokenHelperMock.when(() -> ShareTokenHelper.validateShareToken("valid-token"))
                .thenReturn(true);

        shareTokenHelperMock.when(() -> ShareTokenHelper.getShareTokenBody("valid-token"))
                .thenReturn("{\"projectId\":42,\"userId\":1}");

        UserSharingDTO sharing = new UserSharingDTO();
        sharing.setMember("Alice");
        sharing.setTotalAmount(new BigDecimal("100"));
        SingleResponse<List<UserSharingDTO>> sharingResp = SingleResponse.of(List.of(sharing));
        when(expenseApplicationService.getProjectSharingDetail(any(ProjectSharingQry.class)))
                .thenReturn(sharingResp);

        SingleResponseVo result = adaptor.getProjectSharingDetail("valid-token");

        assertTrue(result.getStatus() == 0);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getData();
        @SuppressWarnings("unchecked")
        List<UserSharingDTO> rows = (List<UserSharingDTO>) data.get("rows");
        assertEquals(1, rows.size());
        assertEquals("Alice", rows.get(0).getMember());
    }

    @Test
    void getProjectSharingDetail_serviceFailure_shouldReturnFailure() {
        shareTokenHelperMock.when(() -> ShareTokenHelper.validateShareToken("valid-token"))
                .thenReturn(true);

        shareTokenHelperMock.when(() -> ShareTokenHelper.getShareTokenBody("valid-token"))
                .thenReturn("{\"projectId\":42,\"userId\":1}");

        SingleResponse<List<UserSharingDTO>> failResp = SingleResponse.buildFailure("ERR", "计算失败");
        when(expenseApplicationService.getProjectSharingDetail(any(ProjectSharingQry.class)))
                .thenReturn(failResp);

        SingleResponseVo result = adaptor.getProjectSharingDetail("valid-token");

        assertFalse(result.getStatus() == 0);
    }
}
