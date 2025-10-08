package com.github.zavier.user.executor;

import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserLoginCmd;
import com.github.zavier.wx.WxGateWay;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Optional;

@Component
public class UserLoginExe {

    @Resource
    private UserGateway userGateway;
    @Resource
    private WxGateWay wxGateWay;

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

    public SingleResponse<String> loginByWxOrRegister(String code) {
        final String openId = wxGateWay.wxLogin(code);
        final Optional<User> userOpt = userGateway.getByOpenId(openId);
        if (userOpt.isPresent()) {
            final User user = userOpt.get();
            final String token = user.generateToken();
            return SingleResponse.of(token);
        }

        // 不存在则注册
        final User user = saveWxUser(openId);
        final String token = user.generateToken();
        return SingleResponse.of(token);
    }

    private User saveWxUser(String openId) {
        User user = new User();
        final String userName = user.generateWxUserName();
        user.setUserName(userName);
        user.setEmail("");
        user.setPasswordHash(user.generatePasswordHash(userName));
        user.setOpenId(openId);
        userGateway.save(user);
        return user;
    }

}
