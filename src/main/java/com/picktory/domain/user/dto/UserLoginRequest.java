package com.picktory.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 로그인 요청 DTO
 * 카카오 인증 코드 정보를 담고 있습니다.
 */
@Getter
@NoArgsConstructor
public class UserLoginRequest {
    /**
     * 카카오 인증 코드
     */
    private String code;
}