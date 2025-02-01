package com.picktory.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;


@Component
public class JwtTokenProvider {

    private final Key key;
    private final long VALID_MILISECOND = 1000L * 60 * 60; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24; // 24시간
    private static final String BEARER_PREFIX = "Bearer ";

    public JwtTokenProvider(@Value("${spring.security.jwt.secret}") String secretkey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretkey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUserName(String jwtToken){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwtToken)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String jwtToken){
        try{
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwtToken);
            return !claims.getBody().getExpiration().before(new Date());
        }catch (Exception e){
            return false;
        }
    }

    public Authentication getAuthentication(String jwtToken) {

        String email = getUserName(jwtToken);

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");

        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singleton(authority)
        );
    }
    private Claims createClaims(String userName){
        Claims claims = Jwts.claims();
        claims.setSubject(userName);
        claims.setIssuedAt(new Date());

        return claims;
    }

    public JwTokenDto generateToken(String userName){
        Claims claims = createClaims(userName);
        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + VALID_MILISECOND);

        String accessToken = Jwts.builder()
                .setClaims(claims) // 발행 유저 정보 저장
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setExpiration(new Date(now + REFRESH_TOKEN_VALIDITY))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return new JwTokenDto(BEARER_PREFIX, accessToken, refreshToken, accessTokenExpiresIn);
    }
}
