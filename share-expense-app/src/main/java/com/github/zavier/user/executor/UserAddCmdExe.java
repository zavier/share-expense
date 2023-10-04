package com.github.zavier.user.executor;

import com.alibaba.cola.dto.Response;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.domainservice.UserValidator;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserAddCmd;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class UserAddCmdExe {

    @Resource
    private UserGateway userGateway;
    @Resource
    private UserValidator userValidator;

    public Response execute(UserAddCmd userAddCmd) {
        validate(userAddCmd);

        User user = new User();
        user.setUsername(userAddCmd.getUsername());
        user.setEmail(userAddCmd.getEmail());
        user.setPasswordHash(user.generatePasswordHash(userAddCmd.getPassword()));
        userGateway.save(user);
        return Response.buildSuccess();
    }

    private void validate(UserAddCmd userAddCmd) {
        userValidator.validateUserName(userAddCmd.getUsername());
        userValidator.validateEmail(userAddCmd.getEmail());
        userValidator.validatePassword(userAddCmd.getPassword());
    }
}
