package com.picktory.domain.refreshToken.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 리프레시 토큰 응답 DTO
 * 클라이언트에게 리프레시 토큰 정보를 전달하기 위한 객체
 */
@Getter
@Builder
public class RefreshTokenResponse {

    private String userId;
    private String token;
    private LocalDateTime expiryDate;

    /**
     * 리프레시 토큰 응답 객체를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param token 리프레시 토큰 문자열
     * @param expiryDate 만료 일시
     * @return 리프레시 토큰 응답 객체
     */
    public static RefreshTokenResponse of(String userId, String token, LocalDateTime expiryDate) {
        return RefreshTokenResponse.builder()
                .userId(userId)
                .token(token)
                .expiryDate(expiryDate)
                .build();
    }
}