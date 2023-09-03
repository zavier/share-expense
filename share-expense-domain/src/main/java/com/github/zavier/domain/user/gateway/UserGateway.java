package com.github.zavier.domain.user.gateway;

import com.github.zavier.domain.user.User;

import java.util.Optional;

public interface UserGateway {

    Optional<User> getByUserName(String username);

    Optional<User> getByEmail(String email);

    User save(User user);
}
