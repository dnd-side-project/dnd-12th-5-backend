package com.picktory.config.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        log.debug("Current request path: {}", path);

        boolean shouldNotFilter = path.equals("/") ||
                path.equals("/api/v1") ||
                path.equals("/api/v1/") ||
                path.startsWith("/api/v1/oauth/login") ||
                path.startsWith("/api/v1/auth/backup") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/kakao/callback") ||
                path.equals("/favicon.ico") ||
                path.equals("/default-ui.css") ||
                path.startsWith("/api/v1/responses/bundles/") ||
                path.startsWith("/api/v1/gifts/");
        log.debug("Should not filter request: {}", shouldNotFilter);
        return shouldNotFilter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwtToken = parseJwt(request);
            log.debug("JWT Token present: {}", jwtToken != null);

            if (jwtToken == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (jwtTokenProvider.validateToken(jwtToken)) {
                processValidToken(jwtToken);
            } else {
                processExpiredToken(jwtToken, response);
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.error("Token expired", e);
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "토큰이 만료되었습니다.");
        } catch (JwtException e) {
            log.error("Invalid token", e);
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "유효한 토큰이 아닙니다.");
        } catch (Exception e) {
            log.error("Filter error", e);
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 오류가 발생했습니다.");
        }
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(BEARER_PREFIX)) {
            return headerAuth.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void processValidToken(String token) {
        Authentication auth = jwtTokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("Valid token processed for user: {}", auth.getName());
    }

    private void processExpiredToken(String oldToken, HttpServletResponse response) {
        String username = jwtTokenProvider.getUserName(oldToken);
        JwTokenDto newToken = jwtTokenProvider.generateToken(username);
        response.setHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + newToken.getAccessToken());

        Authentication auth = jwtTokenProvider.getAuthentication(newToken.getAccessToken());
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("Expired token renewed for user: {}", username);
    }
}