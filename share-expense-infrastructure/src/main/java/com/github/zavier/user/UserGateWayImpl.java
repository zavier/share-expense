package com.github.zavier.user;

import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Optional;

@Repository
public class UserGateWayImpl implements UserGateway {
    @Resource
    private UserMapper userMapper;


    @Override
    public Optional<User> getByUserName(@NotNull String username) {
        final UserDO userDO = new UserDO();
        userDO.setUsername(username);
        return getByUserDo(userDO);
    }

    @Override
    public Optional<User> getByEmail(@NotNull String email) {
        final UserDO userDO = new UserDO();
        userDO.setEmail(email);
        return getByUserDo(userDO);
    }

    private Optional<User> getByUserDo(UserDO userDO) {
        final Optional<UserDO> userDOOpt = userMapper.selectOne(userDO);
        if (!userDOOpt.isPresent()) {
            return Optional.empty();
        }

        final User user = new User();
        user.setUserId(userDOOpt.get().getId());
        user.setUsername(userDOOpt.get().getUsername());
        user.setEmail(userDOOpt.get().getEmail());
        user.setPasswordHash(userDOOpt.get().getPasswordHash());
        return Optional.of(user);
    }

    @Override
    public User save(User user) {
        final UserDO userDO = new UserDO();
        userDO.setUsername(user.getUsername());
        userDO.setEmail(user.getEmail());
        userDO.setPasswordHash(user.getPasswordHash());
        userDO.setCreatedAt(new Date());
        userDO.setUpdatedAt(new Date());
        userMapper.insertSelective(userDO);
        user.setUserId(userDO.getId());
        return user;
    }
}
