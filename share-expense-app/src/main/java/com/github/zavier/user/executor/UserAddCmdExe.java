package com.github.zavier.user.executor;

import com.alibaba.cola.dto.Response;
import com.github.zavier.domain.user.domainservice.UserValidator;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserAddCmd;
import com.github.zavier.dto.data.UserDto;
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
        // TODO
        return null;

//        userGateway.getByEmail()
    }

    private void validate(UserAddCmd userAddCmd) {
        userValidator.validateUserName(userAddCmd.getUsername());
        userValidator.validateEmail(userAddCmd.getEmail());
        userValidator.validatePassword(userAddCmd.getPassword());
    }
}
