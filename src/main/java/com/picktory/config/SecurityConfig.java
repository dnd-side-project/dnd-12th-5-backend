package com.picktory.config;

import com.picktory.config.jwt.filter.JwtAuthenticationFilter;
import com.picktory.config.jwt.filter.JwtExceptionFilter;
import com.picktory.config.jwt.handler.JwtAccessDeniedHandler;
import com.picktory.config.jwt.handler.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtExceptionFilter jwtExceptionFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final String API_V1 = "/api/v1/";

    private enum CorsConfig {
        INSTANCE;

        private final List<String> ALLOWED_ORIGINS = List.of(
                "https://picktory.net",
                "https://www.picktory.net",
                "http://localhost:3000",
                "http://localhost:8080",
                "https://api.picktory.net/",
                "https://api.picktory.net"
        );

        private final List<String> ALLOWED_HEADERS = List.of(
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",
                "X-Requested-With"
        );

        private final List<String> ALLOWED_METHODS = List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        );

        public List<String> getAllowedOrigins() {
            return ALLOWED_ORIGINS;
        }

        public List<String> getAllowedHeaders() {
            return ALLOWED_HEADERS;
        }

        public List<String> getAllowedMethods() {
            return ALLOWED_METHODS;
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .headers(header -> header.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(getPublicEndpoints()).permitAll()
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers(getAuthenticatedEndpoints()).authenticated()
                        .anyRequest().authenticated()
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class)
                .build();
    }

    private String[] getPublicEndpoints() {
        return new String[]{
                "/",
                API_V1,
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/favicon.ico",
                "/default-ui.css",
                "/kakao/callback",
                API_V1 + "oauth/login",
                API_V1 + "auth/backup/signup",
                API_V1 + "auth/backup/login",
                API_V1 + "gifts/{link}/**",
                API_V1 + "responses/bundles/**"
        };
    }

    private String[] getAuthenticatedEndpoints() {
        return new String[]{
                API_V1 + "oauth/logout",
                API_V1 + "user/me",
                API_V1 + "user/me/backup",
                API_V1 + "bundles/**", // 중복 패턴 통합
                API_V1 + "bundles/{id}/save",
                API_V1 + "bundles/{id}/gifts/**",
                API_V1 + "bundles/{id}/delivery",
                API_V1 + "bundles/{id}/deliver"
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(CorsConfig.INSTANCE.getAllowedOrigins());
        configuration.setAllowedHeaders(CorsConfig.INSTANCE.getAllowedHeaders());
        configuration.setAllowedMethods(CorsConfig.INSTANCE.getAllowedMethods());
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}