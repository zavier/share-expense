package com.github.zavier.user;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.Response;
import com.github.zavier.api.UserService;
import com.github.zavier.dto.UserAddCmd;
import org.springframework.stereotype.Service;

@Service
@CatchAndLog
public class UserServiceImpl implements UserService {

    @Override
    public Response addUser(UserAddCmd userAddCmd) {
        return null;
    }
}
