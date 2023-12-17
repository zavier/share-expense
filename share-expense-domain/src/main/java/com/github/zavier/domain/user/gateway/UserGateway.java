package com.github.zavier.domain.user.gateway;

import com.alibaba.cola.dto.PageResponse;
import com.github.zavier.domain.user.User;
import com.github.zavier.dto.UserListQry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface UserGateway {

    Optional<User> getByUserName(String username);

    Optional<User> getUserById(@NotNull Integer userId);

    Optional<User> getByEmail(String email);

    User save(User user);

    PageResponse<User> pageUser(UserListQry userListQry);
}
