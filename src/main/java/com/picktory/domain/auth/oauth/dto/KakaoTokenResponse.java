package com.picktory.domain.auth.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카카오 OAuth 토큰 API 응답을 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KakaoTokenResponse {
    /**
     * 카카오 액세스 토큰
     */
    private String access_token;

    /**
     * 토큰 타입 (bearer)
     */
    private String token_type;

    /**
     * 카카오 리프레시 토큰
     */
    private String refresh_token;

    /**
     * 액세스 토큰 만료 시간 (초)
     */
    private String expires_in;

    /**
     * 토큰 스코프
     */
    private String scope;

    /**
     * 리프레시 토큰 만료 시간 (초)
     */
    private String refresh_token_expires_in;
}