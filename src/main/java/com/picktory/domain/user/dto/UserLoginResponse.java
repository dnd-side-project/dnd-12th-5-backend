package com.picktory.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 로그인 응답 DTO
 * JWT 액세스 토큰과 리프레시 토큰 정보를 담고 있습니다.
 */
@Getter
@Builder
@AllArgsConstructor
public class UserLoginResponse {
    /**
     * JWT 액세스 토큰
     */
    private String accessToken;

    /**
     * JWT 리프레시 토큰
     */
    private String refreshToken;
}