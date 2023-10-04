package com.github.zavier.api;

import com.alibaba.cola.dto.Response;
import com.github.zavier.dto.UserAddCmd;

public interface UserService {

    Response addUser(UserAddCmd userAddCmd);

}
