package com.github.zavier.user;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.UnitTestBase;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.domainservice.PasswordEncoder;
import com.github.zavier.domain.user.domainservice.TokenProvider;
import com.github.zavier.domain.user.domainservice.UserValidator;
import com.github.zavier.domain.user.gateway.UserGateway;
import com.github.zavier.dto.UserAddCmd;
import com.github.zavier.dto.UserLoginCmd;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserApplicationServiceTest extends UnitTestBase {

    @Mock
    private UserGateway userGateway;

    @Mock
    private UserValidator userValidator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private UserApplicationService userApplicationService;

    @Test
    void addUser_validCmd_shouldReturnSuccess() {
        UserAddCmd cmd = new UserAddCmd();
        cmd.setUsername("testuser");
        cmd.setEmail("test@example.com");
        cmd.setPassword("password123");

        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userGateway.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(1);
            return u;
        });

        Response response = userApplicationService.addUser(cmd);

        assertTrue(response.isSuccess());
        verify(userValidator).validateUserName("testuser");
        verify(userValidator).validateEmail("test@example.com");
        verify(userValidator).validatePassword("password123");
        verify(passwordEncoder).encode("password123");
        verify(userGateway).save(any(User.class));
    }

    @Test
    void login_invalidCredentials_shouldReturnFailure() {
        UserLoginCmd cmd = new UserLoginCmd();
        cmd.setUsername("nonexistent");
        cmd.setPassword("wrong");

        when(userGateway.getByUserName("nonexistent")).thenReturn(Optional.empty());

        SingleResponse<String> response = userApplicationService.login(cmd);

        assertFalse(response.isSuccess());
        assertEquals("-1", response.getErrCode());
    }

    @Test
    void login_validCredentials_shouldReturnToken() {
        UserLoginCmd cmd = new UserLoginCmd();
        cmd.setUsername("testuser");
        cmd.setPassword("correct");

        User mockUser = mock(User.class);
        when(mockUser.checkPassword("correct", passwordEncoder)).thenReturn(true);
        when(mockUser.generateToken(tokenProvider)).thenReturn("jwt-token-123");

        when(userGateway.getByUserName("testuser")).thenReturn(Optional.of(mockUser));

        SingleResponse<String> response = userApplicationService.login(cmd);

        assertTrue(response.isSuccess());
        assertEquals("jwt-token-123", response.getData());
    }

    @Test
    void listUser_validQuery_shouldReturnPage() {
        com.github.zavier.dto.UserListQry qry = new com.github.zavier.dto.UserListQry();
        qry.setPage(1);
        qry.setSize(10);

        User user = new User();
        user.setUserId(1);
        user.setUserName("testuser");
        user.setEmail("test@example.com");

        PageResponse<User> userPage = PageResponse.of(Collections.singletonList(user), 1, 10, 1);
        when(userGateway.pageUser(any())).thenReturn(userPage);

        var response = userApplicationService.listUser(qry);

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("testuser", response.getData().get(0).getUserName());
    }
}
