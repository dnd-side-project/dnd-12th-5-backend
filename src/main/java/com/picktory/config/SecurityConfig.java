package com.picktory.config;

import com.picktory.config.jwt.JwtAuthenticationFilter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public static final String API_V_1 = "/api/v1/";
    private static final List<String> ORIGIN_PATTERN = List.of("https://picktory.net");
    private static final String CORS_CONFIGURATION_PATTERN = "/**";

    private static final List<String> ALLOWED_HEADERS = Arrays.asList(
            "Origin", "Content-Type", "Accept", "Authorization", "X-Requested-With"
    );
    private static final List<String> ALLOWED_METHODS = Arrays.asList(
            "GET", "POST", "PUT", "DELETE"
    );

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                        // 루트 URL(/) 접근 허용 (/login으로 자동 리디렉트되지 않음)
                        .requestMatchers("/", "/favicon.ico", "/default-ui.css").permitAll()

                        // Swagger & Preflight
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()

                        // Auth 관련
                        .requestMatchers(API_V_1 + "oauth/login").permitAll()            // 카카오 로그인
                        .requestMatchers(API_V_1 + "auth/backup/signup").permitAll()     // 백업 계정 가입
                        .requestMatchers(API_V_1 + "auth/backup/login").permitAll()      // 백업 계정 로그인
                        .requestMatchers(API_V_1 + "oauth/logout").authenticated()        // 로그아웃

                        // User 관련
                        .requestMatchers(API_V_1 + "user/me").authenticated()            // 내 정보 조회/수정/삭제
                        .requestMatchers(API_V_1 + "user/me/backup").authenticated()     // 백업 계정 정보 수정

                        // Bundle & Gift 관련
                        .requestMatchers(API_V_1 + "bundles").authenticated()                    // 보따리 생성/조회
                        .requestMatchers(API_V_1 + "bundles/main").authenticated()               // 메인화면 보따리 목록
                        .requestMatchers(API_V_1 + "bundles/{id}/**").authenticated()            // 보따리 상세/수정/삭제
                        .requestMatchers(API_V_1 + "bundles/{id}/save").authenticated()          // 임시저장
                        .requestMatchers(API_V_1 + "bundles/{id}/gifts/**").authenticated()      // 선물 관리
                        .requestMatchers(API_V_1 + "bundles/{id}/delivery").authenticated()      // 배달부 설정
                        .requestMatchers(API_V_1 + "bundles/{id}/deliver").authenticated()       // 배달 시작

                        // 수신자용 API (링크 접근)
                        .requestMatchers(API_V_1 + "gifts/{link}/**").permitAll()                // 선물 조회/답변

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .headers(header -> header
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .disable()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(httpSecurityCorsConfigurer -> corsConfigurationSource())
                .formLogin(AbstractHttpConfigurer::disable) // /login 자동 리디렉트 방지 추가 설정
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ORIGIN_PATTERN);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowCredentials(true);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(CORS_CONFIGURATION_PATTERN, configuration);

        return source;
    }
}