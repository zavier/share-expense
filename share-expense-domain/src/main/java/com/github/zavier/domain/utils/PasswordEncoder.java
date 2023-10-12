package com.github.zavier.domain.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordEncoder {

    public static String encode(String pwd) {
        // 生成盐值
        String salt = BCrypt.gensalt();
        // 使用盐值和密码生成哈希值
        return BCrypt.hashpw(pwd, salt);
    }

    public static boolean checkPassword(String pwd, String hash) {
        return BCrypt.checkpw(pwd, hash);
    }
}
