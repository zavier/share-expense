package com.github.zavier.web.filter;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.github.zavier.domain.user.User;

public class UserHolder {

    private static TransmittableThreadLocal<User> userThreadLocal = new TransmittableThreadLocal<>();

    public static void setUser(User user) {
        userThreadLocal.set(user);
    }

    public static User getUser() {
        return userThreadLocal.get();
    }

    public static void clear(){
        userThreadLocal.remove();
    }
}
