package com.github.zavier.infrastructure.user;

import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.user.User;
import com.github.zavier.domain.user.domainservice.TokenProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * JWT Token 提供者实现
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private static final String SECRET_KEY = "y=VFgqXLbQ,55v]H.kB0J=e)*1ND1q:B!,%CZ^";

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    @Override
    public String generateToken(User user) {
        final LocalDateTime localDateTime = LocalDateTime.now().plusDays(30);
        return Jwts.builder().subject(user.getUserName())
                .claim("userId", user.getUserId())
                .issuedAt(new Date())
                .expiration(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(key).compact();
    }

    @Override
    public boolean verifyToken(String token) {
        final JwtParser jwtParser = Jwts.parser().verifyWith(key).build();
        try {
            final Jws<Claims> claims = jwtParser.parseSignedClaims(token);
            final Claims payload = claims.getPayload();
            return !payload.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public User getUser(String token) {
        final JwtParser jwtParser = Jwts.parser().verifyWith(key).build();
        try {
            final Jws<Claims> claims = jwtParser.parseSignedClaims(token);
            final Claims payload = claims.getPayload();
            if (payload.getExpiration().before(new Date())) {
                throw new BizException("当前用户登陆已过期，请重新登陆");
            }

            final String userName = payload.getSubject();
            final Integer userId = payload.get("userId", Integer.class);
            final User user = new User();
            user.setUserId(userId);
            user.setUserName(userName);
            return user;
        } catch (JwtException e) {
            throw new BizException("当前用户登陆已过期，请重新登陆");
        } catch (IllegalArgumentException e) {
            throw new BizException("当前用户未登陆");
        }
    }
}
