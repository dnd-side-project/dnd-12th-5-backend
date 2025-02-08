package com.picktory.support.config.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

/**
 * ✅ 모든 테스트에서 사용할 Mock JWT를 생성하는 클래스
 */
public class TestJwtTokenProvider {

    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256); // ✅ 256비트 키 자동 생성
    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // ✅ 1시간 유효기간

    /**
     * ✅ 테스트용 JWT 생성
     * @param userId 사용할 유저 ID
     * @return JWT 토큰
     */
    public static String generateTestToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY) // ✅ 올바른 키 사용
                .compact();
    }
}
