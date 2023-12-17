package com.github.zavier.domain.user.domainservice;


import com.alibaba.cola.exception.Assert;
import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class UserValidator {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);

    private final UserGateway userGateway;

    public UserValidator(UserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public void validateUserName(String username) {
        Assert.isTrue(StringUtils.isNotBlank(username), "名称不能为空");
        Optional<User> user = userGateway.getByUserName(username);
        if (user.isPresent()) {
            throw new BizException("用户名已存在");
        }
    }

    public void validateEmail(String email) {
        Assert.isTrue(StringUtils.isNotBlank(email), "email不能为空");
        Assert.isTrue(emailPattern.matcher(email).matches(), "email格式错误");
        final Optional<User> userOptional = userGateway.getByEmail(email);
        if (userOptional.isPresent()) {
            throw new BizException("email已存在");
        }
    }

    public void validatePassword(String password) {
        Assert.isTrue(StringUtils.isNotBlank(password), "密码不能为空");
        Assert.isTrue(password.length() >= 5, "密码不能小于5位");
    }

}
