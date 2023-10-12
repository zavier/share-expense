package com.github.zavier.user.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserLoginCmd;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

@Component
public class UserLoginExe {

    @Resource
    private UserGateway userGateway;

    public SingleResponse<String> execute(UserLoginCmd userLoginCmd) {
        Assert.notNull(userLoginCmd.getUsername(), "用户名不能为空");
        Assert.notNull(userLoginCmd.getPassword(), "密码不能为空");

        final Optional<User> userOpt = userGateway.getByUserName(userLoginCmd.getUsername());
        if (!userOpt.isPresent()) {
            return SingleResponse.buildFailure("-1", "用户名/密码错误");
        }

        final User user = userOpt.get();
        final boolean pwdSuccess = user.checkPassword(userLoginCmd.getPassword());
        if (!pwdSuccess) {
            return SingleResponse.buildFailure("-1", "用户名/密码错误");
        }

        final String token = user.generateToken();

        return SingleResponse.of(token);
    }

}
