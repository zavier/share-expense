package com.github.zavier.web.filter;

import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.domainservice.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LoginFilter 测试
 */
public class LoginFilterTest {

    private LoginFilter loginFilter;
    private TokenProvider tokenProvider;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private PrintWriter printWriter;

    @BeforeEach
    public void setUp() throws IOException {
        tokenProvider = mock(TokenProvider.class);
        loginFilter = new LoginFilter(tokenProvider);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        printWriter = mock(PrintWriter.class);

        when(response.getWriter()).thenReturn(printWriter);
    }

    // ==================== 白名单 URL ====================

    @Test
    public void doFilter_loginUrl_shouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/user/login");

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).getWriter();
    }

    @Test
    public void doFilter_userAddUrl_shouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/user/add");

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void doFilter_shareUrl_shouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/share");

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void doFilter_htmlPage_shouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/ai-chat.html");

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void doFilter_staticResource_shouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/static/app.js");

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void doFilter_cssFile_shouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/style.css");

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void doFilter_favicon_shouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/favicon.ico");

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ==================== 需要认证的 URL ====================

    @Test
    public void doFilter_protectedUrl_validToken_shouldProceed() throws Exception {
        when(request.getRequestURI()).thenReturn("/expense/project/list");
        String validToken = "valid-jwt-token";
        Cookie jwtCookie = new Cookie("jwtToken", validToken);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(tokenProvider.verifyToken(validToken)).thenReturn(true);

        User mockUser = new User();
        mockUser.setUserId(1);
        mockUser.setUserName("testuser");
        when(tokenProvider.getUser(validToken)).thenReturn(mockUser);

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).getWriter();
    }

    @Test
    public void doFilter_protectedUrl_invalidToken_shouldReturn401() throws Exception {
        when(request.getRequestURI()).thenReturn("/expense/project/list");
        String invalidToken = "invalid-token";
        Cookie jwtCookie = new Cookie("jwtToken", invalidToken);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(tokenProvider.verifyToken(invalidToken)).thenReturn(false);

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(printWriter).write(contains("unauthorized"));
    }

    @Test
    public void doFilter_protectedUrl_noCookie_shouldReturn401() throws Exception {
        when(request.getRequestURI()).thenReturn("/expense/project/list");
        when(request.getCookies()).thenReturn(null);

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(printWriter).write(contains("unauthorized"));
    }

    @Test
    public void doFilter_protectedUrl_emptyCookies_shouldReturn401() throws Exception {
        when(request.getRequestURI()).thenReturn("/expense/project/list");
        when(request.getCookies()).thenReturn(new Cookie[]{});

        loginFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(printWriter).write(contains("unauthorized"));
    }

    // ==================== Cookie 清理 ====================

    @Test
    public void doFilter_invalidToken_shouldRemoveJwtCookie() throws Exception {
        when(request.getRequestURI()).thenReturn("/expense/project/list");
        String invalidToken = "expired-token";
        Cookie jwtCookie = new Cookie("jwtToken", invalidToken);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(tokenProvider.verifyToken(invalidToken)).thenReturn(false);

        loginFilter.doFilter(request, response, filterChain);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie removedCookie = cookieCaptor.getValue();
        assertEquals("jwtToken", removedCookie.getName());
        assertEquals("/", removedCookie.getPath());
        assertEquals(0, removedCookie.getMaxAge());
    }

    // ==================== UserHolder 清理 ====================

    @Test
    public void doFilter_validToken_shouldClearUserHolderAfterRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/expense/project/list");
        String validToken = "valid-token";
        Cookie jwtCookie = new Cookie("jwtToken", validToken);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(tokenProvider.verifyToken(validToken)).thenReturn(true);

        User mockUser = new User();
        mockUser.setUserId(1);
        mockUser.setUserName("testuser");
        when(tokenProvider.getUser(validToken)).thenReturn(mockUser);

        loginFilter.doFilter(request, response, filterChain);

        // After filter completes, UserHolder should be cleared
        assertNull(UserHolder.getUser());
    }

    @Test
    public void doFilter_exceptionDuringProcessing_shouldStillClearUserHolder() throws Exception {
        when(request.getRequestURI()).thenReturn("/expense/project/list");
        String validToken = "valid-token";
        Cookie jwtCookie = new Cookie("jwtToken", validToken);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});
        when(tokenProvider.verifyToken(validToken)).thenReturn(true);

        User mockUser = new User();
        mockUser.setUserId(1);
        when(tokenProvider.getUser(validToken)).thenReturn(mockUser);
        doThrow(new ServletException("test error")).when(filterChain).doFilter(request, response);

        try {
            loginFilter.doFilter(request, response, filterChain);
        } catch (ServletException e) {
            // expected
        }

        // UserHolder must be cleared even on exception
        assertNull(UserHolder.getUser());
    }
}
