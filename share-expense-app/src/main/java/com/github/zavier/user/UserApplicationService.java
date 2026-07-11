package com.github.zavier.user;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.domainservice.UserValidator;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserAddCmd;
import com.github.zavier.dto.UserListQry;
import com.github.zavier.dto.UserLoginCmd;
import com.github.zavier.dto.data.UserDTO;
import com.github.zavier.wx.WxGateWay;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@CatchAndLog
public class UserApplicationService {

    @Resource
    private UserGateway userGateway;

    @Resource
    private UserValidator userValidator;

    @Resource
    private WxGateWay wxGateWay;

    public Response addUser(UserAddCmd userAddCmd) {
        userValidator.validateUserName(userAddCmd.getUsername());
        userValidator.validateEmail(userAddCmd.getEmail());
        userValidator.validatePassword(userAddCmd.getPassword());

        User user = new User();
        user.setUserName(userAddCmd.getUsername());
        user.setEmail(userAddCmd.getEmail());
        user.setPasswordHash(user.generatePasswordHash(userAddCmd.getPassword()));
        userGateway.save(user);
        return Response.buildSuccess();
    }

    public SingleResponse<UserDTO> getUserById(Integer userId) {
        final UserListQry userListQry = new UserListQry();
        userListQry.setUserId(userId);
        userListQry.setPage(1);
        userListQry.setSize(1);

        final PageResponse<UserDTO> execute = listUser(userListQry);
        if (!execute.isSuccess()) {
            return SingleResponse.buildFailure(execute.getErrCode(), execute.getErrMessage());
        }
        if (CollectionUtils.isEmpty(execute.getData())) {
            return SingleResponse.buildSuccess();
        }
        return SingleResponse.of(execute.getData().get(0));
    }

    public PageResponse<UserDTO> listUser(UserListQry userListQry) {
        Assert.notNull(userListQry.getPage(), "页码不能为空");
        Assert.notNull(userListQry.getSize(), "页大小不能为空");

        final PageResponse<User> userPageResponse = userGateway.pageUser(userListQry);

        final List<UserDTO> collect = userPageResponse.getData().stream()
                .map(it -> {
                    final UserDTO userDTO = new UserDTO();
                    userDTO.setUserId(it.getUserId());
                    userDTO.setUserName(it.getUserName());
                    userDTO.setEmail(it.getEmail());
                    return userDTO;
                }).collect(Collectors.toList());

        return PageResponse.of(collect, userPageResponse.getTotalCount(), userPageResponse.getPageSize(), userPageResponse.getPageIndex());
    }

    public SingleResponse<String> login(UserLoginCmd userLoginCmd) {
        Assert.notNull(userLoginCmd.getUsername(), "用户名不能为空");
        Assert.notNull(userLoginCmd.getPassword(), "密码不能为空");

        final Optional<User> userOpt = userGateway.getByUserName(userLoginCmd.getUsername());
        if (!userOpt.isPresent()) {
            return SingleResponse.buildFailure("-1", "用户名/密码错误");
        }

        final User user = userOpt.get();
        final boolean pwdSuccess = user.checkPassword(userLoginCmd.getPassword());
        if (!pwdSuccess) {
            return SingleResponse.buildFailure("-1", "用户名/密码错误");
        }

        final String token = user.generateToken();
        return SingleResponse.of(token);
    }

    public SingleResponse<String> wxLogin(String code) {
        final String openId = wxGateWay.wxLogin(code);
        final Optional<User> userOpt = userGateway.getByOpenId(openId);
        if (userOpt.isPresent()) {
            final User user = userOpt.get();
            final String token = user.generateToken();
            return SingleResponse.of(token);
        }

        // 不存在则注册
        final User user = saveWxUser(openId);
        final String token = user.generateToken();
        return SingleResponse.of(token);
    }

    private User saveWxUser(String openId) {
        User user = new User();
        final String userName = user.generateWxUserName();
        user.setUserName(userName);
        user.setEmail("");
        user.setPasswordHash(user.generatePasswordHash(userName));
        user.setOpenId(openId);
        userGateway.save(user);
        return user;
    }
}
