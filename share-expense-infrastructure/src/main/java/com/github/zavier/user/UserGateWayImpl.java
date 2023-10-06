package com.github.zavier.user;

import com.alibaba.cola.dto.PageResponse;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.zavier.converter.UserConverter;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserListQry;
import io.mybatis.mapper.example.ExampleWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        return userDOOpt.map(UserConverter::toUser);
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

    @Override
    public PageResponse<User> listUser(UserListQry userListQry){
        PageHelper.startPage(userListQry.getPage(), userListQry.getSize());
        final ExampleWrapper<UserDO, Integer> wrapper = userMapper.wrapper();
        if (StringUtils.isNotBlank(userListQry.getUserName())) {
            wrapper.like(UserDO::getUsername, userListQry.getUserName() + "%");
        }
        if (StringUtils.isNotBlank(userListQry.getEmail())) {
            wrapper.like(UserDO::getEmail, userListQry.getEmail() + "%");
        }
        final List<UserDO> list = wrapper.list();
        final Page<UserDO> page = (Page<UserDO>) list;

        final List<User> userList = list.stream().map(UserConverter::toUser).collect(Collectors.toList());
        return PageResponse.of(userList, (int) page.getTotal(), page.getPageSize(), page.getPageNum());

    }
}
