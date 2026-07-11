package com.github.zavier.infrastructure.user;

import com.github.zavier.domain.user.domainservice.PasswordEncoder;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * BCrypt 密码编码器实现
 */
@Component
public class BCryptPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(String rawPassword) {
        String salt = BCrypt.gensalt();
        return BCrypt.hashpw(rawPassword, salt);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
