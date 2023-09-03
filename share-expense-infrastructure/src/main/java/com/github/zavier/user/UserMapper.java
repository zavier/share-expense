package com.github.zavier.user;

import io.mybatis.mapper.Mapper;

@org.apache.ibatis.annotations.Mapper
public interface UserMapper extends Mapper<UserDO, Integer> {
}
