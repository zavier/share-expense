package com.github.zavier.api;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.dto.UserAddCmd;
import com.github.zavier.dto.UserListQry;
import com.github.zavier.dto.UserLoginCmd;
import com.github.zavier.dto.data.UserDTO;

public interface UserService {

    Response addUser(UserAddCmd userAddCmd);

    PageResponse<UserDTO> listUser(UserListQry userListQry);

    /**
     * 成功返回token
     *
     * @param userLoginCmd
     * @return
     */
    SingleResponse<String> login(UserLoginCmd userLoginCmd);
}
