package com.picktory.config.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwtToken = parseJwt(request);

            if (jwtToken != null) {
                if (jwtTokenProvider.validateToken(jwtToken)) {
                    processValidToken(jwtToken);
                } else {
                    processExpiredToken(jwtToken, response);
                }
            }
        } catch (ExpiredJwtException e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "토큰이 만료되었습니다.");
        } catch (JwtException e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "유효한 토큰이 아닙니다.");
        } catch (Exception e) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 오류가 발생했습니다.");
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(BEARER_PREFIX)) {
            return headerAuth.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    // 토큰 처리 로직 분리
    private void processValidToken(String token) {
        Authentication auth = jwtTokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // 만료 토큰 검증후 재발급
    private void processExpiredToken(String oldToken, HttpServletResponse response) {
        String username = jwtTokenProvider.getUserName(oldToken);
        JwTokenDto newToken = jwtTokenProvider.generateToken(username);
        response.setHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + newToken.getAccessToken());

        Authentication auth = jwtTokenProvider.getAuthentication(newToken.getAccessToken());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
