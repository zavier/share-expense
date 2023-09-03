package com.github.zavier.user;

import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Repository
public class UserGateWayImpl {
    @Resource
    private UserMapper userMapper;


}
