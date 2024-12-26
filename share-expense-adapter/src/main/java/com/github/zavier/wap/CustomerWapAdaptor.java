package com.github.zavier.wap;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.UserService;
import com.github.zavier.vo.SingleResponseVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 用户微信小程序相关功能
 *
 */
@RestController
@RequestMapping("/expense/wx/user")
public class CustomerWapAdaptor {

    @Resource
    private UserService userService;

    @GetMapping("/login")
    public SingleResponseVo wxLogin(@RequestParam String code, HttpServletResponse httpServletResponse) throws IOException {
        if (StringUtils.isBlank(code)) {
            return SingleResponseVo.buildFailure("WX_LOGIN_FAILED", "微信登录失败");
        }

        final SingleResponse<String> tokenResp = userService.wxLogin(code);
        if (!tokenResp.isSuccess()) {
            return SingleResponseVo.buildFailure("WX_LOGIN_FAILED", "微信登录失败");
        }

        // 临时方案
        final Cookie cookie = new Cookie("jwtToken", tokenResp.getData());
        cookie.setPath("/");
        cookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(30));
        httpServletResponse.addCookie(cookie);
        return SingleResponseVo.of(tokenResp.getData());
    }
}
