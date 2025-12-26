package com.github.zavier.user.executor;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserListQry;
import com.github.zavier.dto.data.UserDTO;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserListQryExe {

    @Resource
    private UserGateway userGateway;

    public PageResponse<UserDTO> execute(UserListQry userListQry) {
        Assert.notNull(userListQry.getPage(), "页码不能为空");
        Assert.notNull(userListQry.getSize(), "页大小不能为空");

        final PageResponse<User> userPageResponse = userGateway.pageUser(userListQry);

        final List<UserDTO> collect = userPageResponse.getData().stream()
                .map(it -> {
                    final UserDTO userDTO = new UserDTO();
                    userDTO.setUserId(it.getUserId());
                    userDTO.setUserName(it.getUserName());
                    userDTO.setEmail(it.getEmail());
                    return userDTO;
                }).collect(Collectors.toList());

        return PageResponse.of(collect, userPageResponse.getTotalCount(), userPageResponse.getPageSize(), userPageResponse.getPageIndex());
    }

}
