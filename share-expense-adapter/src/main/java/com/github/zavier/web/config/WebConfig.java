package com.github.zavier.web.config;

import com.github.zavier.domain.user.domainservice.TokenProvider;
import com.github.zavier.web.filter.LoginFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web配置类 - Spring Boot 3.x兼容
 */
@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<LoginFilter> loginFilterRegistration(TokenProvider tokenProvider) {
        FilterRegistrationBean<LoginFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LoginFilter(tokenProvider));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("loginFilter");
        return registration;
    }
}