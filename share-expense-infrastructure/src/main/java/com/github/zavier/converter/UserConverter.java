package com.github.zavier.converter;

import com.github.zavier.domain.user.User;
import com.github.zavier.user.UserDO;

public class UserConverter {

    public static User toUser(UserDO userDO) {
        final User user = new User();
        user.setUserId(userDO.getId());
        user.setUsername(userDO.getUsername());
        user.setEmail(userDO.getEmail());
        user.setPasswordHash(userDO.getPasswordHash());
        return user;
    }
}
