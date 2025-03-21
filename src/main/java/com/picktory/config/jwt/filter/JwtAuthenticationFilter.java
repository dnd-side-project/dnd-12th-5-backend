package com.picktory.config.jwt.filter;

import com.picktory.config.jwt.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // 필터링하지 않을 공개 경로 목록
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/",
            "/api/v1/oauth/login",
            "/api/v1/auth/backup",
            "/swagger-ui",
            "/v3/api-docs",
            "/favicon.ico",
            "/default-ui.css"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        log.debug("Current request path: {}", path);

        boolean shouldNotFilter = PUBLIC_PATHS.stream()
                .anyMatch(publicPath -> path.equals(publicPath) ||
                        (!publicPath.equals("/") && path.startsWith(publicPath)));

        log.debug("Should not filter request: {}", shouldNotFilter);
        return shouldNotFilter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = parseJwt(request);
        log.debug("JWT Token present: {}", token != null);

        if (token != null) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    Authentication auth = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Valid token processed for user: {}", auth.getName());
                }
            } catch (ExpiredJwtException e) {
                log.debug("Handling expired token");
                // 만료된 토큰은 JwtExceptionFilter에서 처리
                throw e;
            }
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
}