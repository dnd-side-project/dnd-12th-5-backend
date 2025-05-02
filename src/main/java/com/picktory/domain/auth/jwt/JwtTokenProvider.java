package com.picktory.domain.auth.jwt;

import com.picktory.domain.auth.dto.TokenDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;

    private static final long ACCESS_TOKEN_VALIDITY = 60 * 60 * 1000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000L; // 7일
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_USER = "ROLE_USER";

    public JwtTokenProvider(@Value("${spring.security.jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT Token Provider initialized with secret key");
    }

    public TokenDto generateToken(Long userId) {
        Date now = new Date();
        Date accessTokenExpiresIn = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY);
        Date refreshTokenExpiresIn = new Date(now.getTime() + REFRESH_TOKEN_VALIDITY);

        // Access Token 생성
        String accessToken = Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성 - userId도 추가
        String refreshToken = Jwts.builder()
                .setSubject(userId.toString())  // userId를 추가
                .setIssuedAt(now)  // 발급 시간 추가
                .setExpiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // TokenDto 생성 시 refreshTokenExpiresIn도 함께 전달
        return TokenDto.of(BEARER_PREFIX, accessToken, refreshToken, accessTokenExpiresIn, refreshTokenExpiresIn);
    }

    public String getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return !claims.getBody().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token");
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token");
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token");
            throw e;
        } catch (JwtException e) {
            log.warn("Invalid JWT token");
            throw e;
        } catch (Exception e) {
            log.error("JWT token validation error", e);
            throw new JwtException("JWT token validation failed");
        }
    }

    public Authentication getAuthentication(String token) {
        String userId = getUserId(token);
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(ROLE_USER);

        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.singleton(authority)
        );
    }
}