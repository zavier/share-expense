package com.github.zavier.user;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.UserService;
import com.github.zavier.dto.UserAddCmd;
import com.github.zavier.dto.UserListQry;
import com.github.zavier.dto.UserLoginCmd;
import com.github.zavier.dto.data.UserDTO;
import com.github.zavier.user.executor.UserAddCmdExe;
import com.github.zavier.user.executor.UserListQryExe;
import com.github.zavier.user.executor.UserLoginExe;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@CatchAndLog
public class UserServiceImpl implements UserService {

    @Resource
    private UserAddCmdExe userAddCmdExe;
    @Resource
    private UserListQryExe userListQryExe;
    @Resource
    private UserLoginExe userLoginExe;

    @Override
    public Response addUser(UserAddCmd userAddCmd) {
        return userAddCmdExe.execute(userAddCmd);
    }


    @Override
    public PageResponse<UserDTO> listUser(UserListQry userListQry){
        return userListQryExe.execute(userListQry);
    }

    @Override
    public SingleResponse<String> login(UserLoginCmd userLoginCmd) {
        return userLoginExe.execute(userLoginCmd);
    }
}
