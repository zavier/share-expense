package com.github.zavier.user;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.Response;
import com.github.zavier.api.UserService;
import com.github.zavier.dto.UserAddCmd;
import com.github.zavier.user.executor.UserAddCmdExe;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@CatchAndLog
public class UserServiceImpl implements UserService {

    @Resource
    private UserAddCmdExe userAddCmdExe;

    @Override
    public Response addUser(UserAddCmd userAddCmd) {
        return userAddCmdExe.execute(userAddCmd);
    }


}
