package com.github.zavier.web;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.UserService;
import com.github.zavier.dto.UserAddCmd;
import com.github.zavier.dto.UserListQry;
import com.github.zavier.dto.UserLoginCmd;
import com.github.zavier.dto.data.UserDTO;
import com.github.zavier.vo.PageResponseVo;
import com.github.zavier.vo.ResponseVo;
import com.github.zavier.vo.SingleResponseVo;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/expense/user")
public class UserController {
    @Resource
    private UserService userService;


    @PostMapping("/add")
    public ResponseVo addUser(@RequestBody UserAddCmd userAddCmd){
        final Response response =  userService.addUser(userAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @GetMapping("/list")
    public PageResponseVo<UserDTO> listUser(UserListQry userListQry){
        final PageResponse<UserDTO> pageResponse =  userService.listUser(userListQry);
        return PageResponseVo.buildFromPageResponse(pageResponse);
    }

    @PostMapping("/login")
    public SingleResponseVo login(@RequestBody UserLoginCmd userLoginCmd, HttpServletResponse httpServletResponse) throws IOException {
        final SingleResponse<String> tokenResp = userService.login(userLoginCmd);
        if (!tokenResp.isSuccess()) {
            return SingleResponseVo.buildFailure(tokenResp.getErrCode(), tokenResp.getErrMessage());
        }

        // 临时方案
        final Cookie cookie = new Cookie("jwtToken", tokenResp.getData());
        cookie.setPath("/");
        cookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(30));
        httpServletResponse.addCookie(cookie);
        return SingleResponseVo.of(tokenResp.getData());
    }

    @PostMapping("/logout")
    public ResponseVo login(HttpServletResponse httpServletResponse) throws IOException {
        final Cookie cookie = new Cookie("jwtToken", "");
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        httpServletResponse.addCookie(cookie);
        return ResponseVo.buildSuccess();
    }


}
