package com.picktory.domain.auth.controller;

import com.picktory.common.BaseResponse;
import com.picktory.domain.user.dto.UserLoginRequest;
import com.picktory.domain.user.dto.UserLoginResponse;
import com.picktory.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 소셜 로그인 API
     *
     * @param request 로그인 요청 (카카오 인증 코드 포함)
     * @return 로그인 응답 (JWT 토큰 포함)
     */
    @PostMapping("/oauth/login")
    public ResponseEntity<BaseResponse<UserLoginResponse>> login(@RequestBody UserLoginRequest request) {
        log.info("Login request received with code");
        UserLoginResponse response = authService.loginWithKakao(request.getCode());
        return ResponseEntity.ok(BaseResponse.success(response, "로그인 성공"));
    }

    /**
     * 토큰 갱신 API
     *
     * @param refreshToken 리프레시 토큰
     * @return 새로운 로그인 응답 (JWT 토큰 포함)
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<BaseResponse<UserLoginResponse>> refreshToken(@RequestHeader("Refresh-Token") String refreshToken) {
        log.info("Token refresh request received");
        UserLoginResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(BaseResponse.success(response, "토큰 갱신 성공"));
    }
}