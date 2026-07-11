package com.github.zavier.web.filter;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.domainservice.CurrentUserProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class UserHolder implements CurrentUserProvider {

    private static final TransmittableThreadLocal<User> userThreadLocal = new TransmittableThreadLocal<>();

    public static void setUser(User user) {
        userThreadLocal.set(user);
    }

    public static User getUser() {
        return userThreadLocal.get();
    }

    public static void clear() {
        userThreadLocal.remove();
    }

    @Override
    public Integer getCurrentUserId() {
        Assert.notNull(userThreadLocal.get(), "用户未登录");
        return userThreadLocal.get().getUserId();
    }
}
