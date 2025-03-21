package com.picktory.domain.user.service.auth;

import com.picktory.domain.oauth.dto.KakaoTokenResponse;
import com.picktory.domain.oauth.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오 소셜 로그인 관련 API 호출을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

    private final RestTemplate restTemplate;

    @Value("${kakao.client.id}")
    private String clientId;

    @Value("${kakao.client.secret}")
    private String adminKey;

    @Value("${kakao.redirect.uri}")
    private String redirectUri;

    private static final String KAKAO_AUTH_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_API_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    /**
     * 카카오 인증 코드로 액세스 토큰을 요청합니다.
     *
     * @param code 카카오 인증 코드
     * @return 카카오 액세스 토큰
     * @throws IllegalStateException 토큰 요청 중 오류 발생 시
     */
    public String getKakaoAccessToken(String code) {
        log.debug("Requesting Kakao access token with code: {}", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        params.add("client_secret", adminKey);

        log.debug("Kakao token request params - client_id: {}, redirect_uri: {}", clientId, redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(KAKAO_AUTH_URL, request, KakaoTokenResponse.class);
            log.info("Kakao token response status: {}", response.getStatusCode());

            if (response.getBody() == null || response.getBody().getAccess_token() == null) {
                log.error("Failed to get Kakao access token: empty response");
                throw new IllegalStateException("카카오 로그인 처리 중 오류가 발생했습니다.");
            }

            return response.getBody().getAccess_token();

        } catch (HttpClientErrorException e) {
            log.error("Kakao token error - Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("다시 로그인해 주세요.");
        } catch (Exception e) {
            log.error("Failed to get Kakao access token", e);
            throw new IllegalStateException("카카오 로그인 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 카카오 액세스 토큰으로 사용자 정보를 요청합니다.
     *
     * @param accessToken 카카오 액세스 토큰
     * @return 카카오 사용자 정보
     * @throws IllegalStateException 사용자 정보 요청 중 오류 발생 시
     */
    public KakaoUserInfo getKakaoUserInfo(String accessToken) {
        try {
            log.debug("Requesting Kakao user info with access token");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                    KAKAO_API_URL,
                    HttpMethod.GET,
                    request,
                    KakaoUserInfo.class
            );

            if (response.getBody() == null) {
                log.error("Kakao user info response is empty");
                throw new IllegalStateException("카카오 API 오류가 발생했습니다.");
            }

            log.info("카카오 유저 정보를 성공적으로 받음");
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Failed to get Kakao user info - Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("카카오 사용자 정보 조회 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Unexpected error while getting Kakao user info", e);
            throw new IllegalStateException("카카오 사용자 정보 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 카카오 계정 연결 해제를 요청합니다.
     *
     * @param kakaoId 카카오 사용자 ID
     * @throws IllegalStateException 연결 해제 중 오류 발생 시
     */
    public void unlinkKakaoAccount(Long kakaoId) {
        try {
            log.debug("Unlinking Kakao account: {}", kakaoId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "KakaoAK " + adminKey);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("target_id_type", "user_id");
            params.add("target_id", String.valueOf(kakaoId));

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            restTemplate.postForEntity(KAKAO_UNLINK_URL, request, String.class);

            log.info("Successfully unlinked Kakao account: {}", kakaoId);

        } catch (Exception e) {
            log.error("Failed to unlink Kakao account: {}", kakaoId, e);
            throw new IllegalStateException("카카오 계정 연결 해제 중 오류가 발생했습니다.");
        }
    }
}