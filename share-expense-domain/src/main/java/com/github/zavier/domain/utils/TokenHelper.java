package com.github.zavier.domain.utils;

import com.alibaba.cola.exception.BizException;
import com.github.zavier.domain.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class TokenHelper {

    private static final String SECRET_KEY = "y=VFgqXLbQ,55v]H.kB0J=e)*1ND1q:B!,%CZ^";

    private static SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    public static String generateToken(User user) {
        // 30天有效
        final LocalDateTime localDateTime = LocalDateTime.now().plusDays(30);
        return Jwts.builder().subject(user.getUserName())
                .claim("userId", user.getUserId())
                .issuedAt(new Date())
                .expiration(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(key).compact();
    }

    public static boolean verifyToken(String token) {
        final JwtParser jwtParser = Jwts.parser().verifyWith(key).build();
        try {
            final Jws<Claims> claims = jwtParser.parseSignedClaims(token);
            final Claims payload = claims.getPayload();
            if (payload.getExpiration().before(new Date())) {
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public static User getUser(String token) {
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
