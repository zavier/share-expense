package com.github.zavier.web.filter;

import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.domainservice.CurrentUserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserHolder 作为 CurrentUserProvider 实现的测试
 */
@DisplayName("UserHolder 当前用户提供者测试")
class UserHolderTest {

    private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        currentUserProvider = new UserHolder();
    }

    @AfterEach
    void tearDown() {
        UserHolder.clear();
    }

    @Test
    @DisplayName("有用户时应该返回正确的用户ID")
    void testGetCurrentUserId_WithUser_ShouldReturnUserId() {
        User user = new User();
        user.setUserId(42);
        UserHolder.setUser(user);

        Integer userId = currentUserProvider.getCurrentUserId();

        assertEquals(42, userId, "应该返回当前用户的ID");
    }

    @Test
    @DisplayName("无用户时应该抛出异常")
    void testGetCurrentUserId_WithoutUser_ShouldThrowException() {
        assertThrows(RuntimeException.class, () -> currentUserProvider.getCurrentUserId(),
                "未登录时应该抛出异常");
    }
}
