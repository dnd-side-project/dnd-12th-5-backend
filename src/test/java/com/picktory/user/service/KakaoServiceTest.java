package com.picktory.user.service;

import com.picktory.domain.oauth.dto.KakaoTokenResponse;
import com.picktory.domain.oauth.dto.KakaoUserInfo;
import com.picktory.domain.user.service.auth.KakaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KakaoServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private KakaoService kakaoService;

    private final String KAKAO_AUTH_URL = "https://kauth.kakao.com/oauth/token";
    private final String KAKAO_API_URL = "https://kapi.kakao.com/v2/user/me";
    private final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    @BeforeEach
    void setUp() {
        // 설정값 주입
        ReflectionTestUtils.setField(kakaoService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(kakaoService, "adminKey", "test-admin-key");
        ReflectionTestUtils.setField(kakaoService, "redirectUri", "http://localhost:8080/callback");
    }

    /**
     * 카카오 액세스 토큰 요청 테스트 - 성공 케이스
     */
    @Test
    void testGetKakaoAccessToken_Success() {
        // 테스트 데이터 설정
        String code = "test-code";

        // 응답 객체 설정
        KakaoTokenResponse tokenResponse = KakaoTokenResponse.builder()
                .access_token("test-access-token")
                .token_type("bearer")
                .refresh_token("test-refresh-token")
                .expires_in("3600")
                .scope("profile")
                .refresh_token_expires_in("86400")
                .build();

        // 모킹 설정
        ResponseEntity<KakaoTokenResponse> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(
                eq(KAKAO_AUTH_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenReturn(responseEntity);

        // 실행
        String accessToken = kakaoService.getKakaoAccessToken(code);

        // 검증
        assertThat(accessToken).isEqualTo("test-access-token");
        verify(restTemplate).postForEntity(
                eq(KAKAO_AUTH_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        );
    }

    /**
     * 카카오 액세스 토큰 요청 테스트 - 응답 없음 케이스
     */
    @Test
    void testGetKakaoAccessToken_EmptyResponse() {
        // 테스트 데이터 설정
        String code = "test-code";

        // 빈 응답 객체 설정
        KakaoTokenResponse emptyResponse = KakaoTokenResponse.builder().build();

        // 모킹 설정
        ResponseEntity<KakaoTokenResponse> responseEntity = new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(
                eq(KAKAO_AUTH_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenReturn(responseEntity);

        // 실행 및 예외 검증
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            kakaoService.getKakaoAccessToken(code);
        });

        assertThat(exception.getMessage()).contains("카카오 로그인 처리 중 오류가 발생했습니다");
    }

    /**
     * 카카오 액세스 토큰 요청 테스트 - HTTP 오류 케이스
     */
    @Test
    void testGetKakaoAccessToken_HttpError() {
        // 테스트 데이터 설정
        String code = "test-code";

        // HTTP 오류 모킹
        HttpClientErrorException httpError = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");
        when(restTemplate.postForEntity(
                eq(KAKAO_AUTH_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenThrow(httpError);

        // 실행 및 예외 검증
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            kakaoService.getKakaoAccessToken(code);
        });

        assertThat(exception.getMessage()).contains("다시 로그인해 주세요");
    }

    /**
     * 카카오 사용자 정보 요청 테스트 - 성공 케이스
     */
    @Test
    void testGetKakaoUserInfo_Success() {
        // 테스트 데이터 설정
        String accessToken = "test-access-token";

        // 카카오 사용자 정보 설정
        KakaoUserInfo.KakaoAccount.Profile profile = KakaoUserInfo.KakaoAccount.Profile.builder()
                .nickname("TestUser")
                .build();
        KakaoUserInfo.KakaoAccount account = KakaoUserInfo.KakaoAccount.builder()
                .profile(profile)
                .build();
        KakaoUserInfo userInfo = KakaoUserInfo.builder()
                .id(12345L)
                .kakaoAccount(account)
                .build();

        // 모킹 설정
        ResponseEntity<KakaoUserInfo> responseEntity = new ResponseEntity<>(userInfo, HttpStatus.OK);
        when(restTemplate.exchange(
                eq(KAKAO_API_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(KakaoUserInfo.class)
        )).thenReturn(responseEntity);

        // 실행
        KakaoUserInfo result = kakaoService.getKakaoUserInfo(accessToken);

        // 검증
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(12345L);
        assertThat(result.getKakaoAccount().getProfile().getNickname()).isEqualTo("TestUser");
    }

    /**
     * 카카오 계정 연결 해제 테스트
     */
    @Test
    void testUnlinkKakaoAccount() {
        // 테스트 데이터 설정
        Long kakaoId = 12345L;

        // 모킹 설정
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"id\":12345}", HttpStatus.OK);
        when(restTemplate.postForEntity(
                eq(KAKAO_UNLINK_URL),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        // 실행
        kakaoService.unlinkKakaoAccount(kakaoId);

        // 검증
        verify(restTemplate).postForEntity(
                eq(KAKAO_UNLINK_URL),
                any(HttpEntity.class),
                eq(String.class)
        );
    }
}