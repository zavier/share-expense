package com.github.zavier.web;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.UserService;
import com.github.zavier.dto.UserAddCmd;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;


    @PostMapping("/add")
    public Response addUser(@RequestBody UserAddCmd userAddCmd){
        return userService.addUser(userAddCmd);
    }
}
