package com.github.zavier.domain.utils;

import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.user.User;
import io.jsonwebtoken.*;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class TokenHelper {

    // 使用自己的密钥可以改成： SecretKey key = Keys.hmacShaKeyFor(keyBytes);
    private static SecretKey key = Jwts.SIG.HS256.key().build();

    public static String generateToken(User user) {
        // 30天有效
        final LocalDateTime localDateTime = LocalDateTime.now().plusDays(30);
        return Jwts.builder().subject(user.getUsername())
                .claim("userId", user.getUserId())
                .issuedAt(new Date())
                .expiration(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(key).compact();
    }

    public static User verifyToken(String token) {
        final JwtParser jwtParser = Jwts.parser().verifyWith(key).build();
        try {
            final Jws<Claims> claimsJws = jwtParser.parseSignedClaims(token);
            final Claims payload = claimsJws.getPayload();
            final String userName = payload.getSubject();
            final Integer userId = payload.get("userId", Integer.class);
            final User user = new User();
            user.setUserId(userId);
            user.setUsername(userName);
            return user;
        } catch (JwtException e) {
            throw new BizException("当前用户登陆已过期，请重新登陆");
        } catch (IllegalArgumentException e) {
            throw new BizException("当前用户未登陆");
        }
    }
}
