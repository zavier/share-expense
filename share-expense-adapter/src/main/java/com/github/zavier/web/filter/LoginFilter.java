package com.github.zavier.web.filter;

import com.github.zavier.domain.utils.TokenHelper;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * 登录过滤器 - 兼容Spring Boot 3.x
 * 通过FilterRegistrationBean注册，避免Spring组件扫描问题
 * TODO 后续看是否切换到Spring Security
 */
public class LoginFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化方法，可以为空
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        final String requestURI = httpServletRequest.getRequestURI();
        if (notNeedLogin(requestURI)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final boolean verifyToken = verifyToken(httpServletRequest);
        if (!verifyToken) {
            removeJwtCookieIfPresent(httpServletRequest, httpServletResponse);
            write401(httpServletResponse);
            return;
        }

        try {
            // 设置用户信息到上下文
            Optional<String> token = getJwtToken(httpServletRequest);
            if (token.isPresent()) {
                UserHolder.setUser(TokenHelper.getUser(token.get()));
            }

            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserHolder.clear();
        }
    }

    @Override
    public void destroy() {
        // 销毁方法，可以为空
    }

    private boolean verifyToken(HttpServletRequest httpServletRequest) {
        final Optional<Cookie> jwtCookie = getJwtCookie(httpServletRequest);
        return jwtCookie.filter(cookie -> TokenHelper.verifyToken(cookie.getValue())).isPresent();
    }

    private Optional<String> getJwtToken(HttpServletRequest httpServletRequest) {
        final Optional<Cookie> jwtCookie = getJwtCookie(httpServletRequest);
        return jwtCookie.map(Cookie::getValue);
    }

    private Optional<Cookie> getJwtCookie(HttpServletRequest httpServletRequest) {
        final Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            final String name = cookie.getName();
            if ("jwtToken".equals(name)) {
                return Optional.of(cookie);
            }
        }
        return Optional.empty();
    }

    private void removeJwtCookieIfPresent(HttpServletRequest httpServletRequest, HttpServletResponse response) {
        final Optional<Cookie> jwtCookie = getJwtCookie(httpServletRequest);
        if (jwtCookie.isPresent()) {
            final Cookie cookie = jwtCookie.get();
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }

    private void write401(HttpServletResponse servletResponse) throws IOException {
        servletResponse.setContentType("application/json;charset=UTF-8");
        servletResponse.getWriter().write("{\"status\":-1,\"msg\":\"unauthorized\"}");
        servletResponse.flushBuffer();
    }

    private boolean notNeedLogin(String url) {
        return url.contains("user/login")
                || url.contains("user/add")
                || url.endsWith("/share")
                || url.contains("/api/ai/")
                || url.contains("ai-chat.html")
                || "/".equals(url)
                || url.endsWith(".html")
                || url.endsWith(".css")
                || url.endsWith(".png")
                || url.endsWith(".json")
                || url.endsWith(".js")
                || url.contains("static")
                || url.contains("favicon");
    }
}