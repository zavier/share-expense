package com.github.zavier.user.executor;

import com.alibaba.cola.dto.Response;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.domainservice.UserValidator;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserAddCmd;
import org.springframework.stereotype.Component;

@Component
public class UserAddCmdExe {

    private final UserGateway userGateway;
    private final UserValidator userValidator;

    public UserAddCmdExe(UserGateway userGateway, UserValidator userValidator) {
        this.userGateway = userGateway;
        this.userValidator = userValidator;
    }

    public Response execute(UserAddCmd userAddCmd) {
        validate(userAddCmd);

        User user = new User();
        user.setUserName(userAddCmd.getUsername());
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
