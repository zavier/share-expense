package com.github.zavier.web;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.github.zavier.api.UserService;
import com.github.zavier.dto.UserAddCmd;
import com.github.zavier.dto.UserListQry;
import com.github.zavier.dto.data.UserDTO;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/list")
    public PageResponse<UserDTO> listUser(UserListQry userListQry){
        return userService.listUser(userListQry);
    }
}
