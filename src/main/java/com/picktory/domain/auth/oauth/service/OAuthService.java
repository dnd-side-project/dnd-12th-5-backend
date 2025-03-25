package com.picktory.domain.auth.oauth.service;

import com.picktory.domain.auth.oauth.client.KakaoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * OAuth 인증 관련 서비스
 * 다양한 소셜 로그인 지원을 위한 공통 기능 및 팩토리 역할
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final KakaoClient kakaoClient;

    /**
     * OAuth 제공자 유형
     */
    public enum ProviderType {
        KAKAO,
        GOOGLE,
        NAVER
        // 추가 제공자는 필요에 따라 확장
    }

    /**
     * 소셜 로그인 프로세스 처리
     * 제공자 유형에 따라 적절한 서비스로 위임
     *
     * @param providerType OAuth 제공자 유형
     * @param code 인증 코드
     * @return 사용자 정보 (제공자별 구현에 따라 다름)
     */
    public Object processOAuthLogin(ProviderType providerType, String code) {
        log.info("Processing OAuth login for provider: {}", providerType);

        return switch (providerType) {
            case KAKAO -> {
                String accessToken = kakaoClient.getKakaoAccessToken(code);
                yield kakaoClient.getKakaoUserInfo(accessToken);
            }
            case GOOGLE, NAVER -> {
                log.warn("Provider {} not implemented yet", providerType);
                throw new UnsupportedOperationException("해당 소셜 로그인은 아직 지원되지 않습니다: " + providerType);
            }
        };
    }

    /**
     * 소셜 계정 연결 해제
     *
     * @param providerType OAuth 제공자 유형
     * @param providerUserId 제공자의 사용자 ID
     */
    public void unlinkSocialAccount(ProviderType providerType, String providerUserId) {
        log.info("Unlinking social account for provider: {}, userId: {}", providerType, providerUserId);

        switch (providerType) {
            case KAKAO -> kakaoClient.unlinkKakaoAccount(Long.parseLong(providerUserId));
            case GOOGLE, NAVER ->
                    throw new UnsupportedOperationException("해당 소셜 로그인은 아직 지원되지 않습니다: " + providerType);
        }
    }
}