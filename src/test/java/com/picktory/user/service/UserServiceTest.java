package com.picktory.user.service;

import com.picktory.config.jwt.JwTokenDto;
import com.picktory.config.jwt.JwtTokenProvider;
import com.picktory.domain.user.dto.KakaoTokenResponse;
import com.picktory.domain.user.dto.KakaoUserInfo;
import com.picktory.domain.user.dto.UserLoginResponse;
import com.picktory.domain.user.dto.UserResponse;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
import com.picktory.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserService userService;

    /**
     * Kakao 로그인 기능 테스트 - 신규 유저인 경우
     */
    @Test
    void testLogin_NewUser() throws Exception {
        String code = "dummyCode";

        // 1. Kakao Access Token 응답 모킹
        KakaoTokenResponse tokenResponse = KakaoTokenResponse.builder()
                .access_token("dummyAccessToken")
                .token_type("bearer")
                .refresh_token("dummyRefreshToken")
                .expires_in("3600")
                .scope("profile")
                .refresh_token_expires_in("86400")
                .build();
        ResponseEntity<KakaoTokenResponse> tokenResponseEntity =
                new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(
                eq("https://kauth.kakao.com/oauth/token"),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenReturn(tokenResponseEntity);

        // 2. Kakao User Info 응답 모킹
        KakaoUserInfo.KakaoAccount.Profile profile = KakaoUserInfo.KakaoAccount.Profile.builder()
                .nickname("TestUser")
                .build();
        KakaoUserInfo.KakaoAccount account = KakaoUserInfo.KakaoAccount.builder()
                .profile(profile)
                .build();
        KakaoUserInfo kakaoUserInfo = KakaoUserInfo.builder()
                .id(12345L)
                .kakaoAccount(account)
                .build();
        ResponseEntity<KakaoUserInfo> userInfoResponseEntity =
                new ResponseEntity<>(kakaoUserInfo, HttpStatus.OK);
        when(restTemplate.exchange(
                eq("https://kapi.kakao.com/v2/user/me"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(KakaoUserInfo.class)
        )).thenReturn(userInfoResponseEntity);

        // 3. 신규 유저 생성을 위해 기존 유저가 없음을 시뮬레이션
        when(userRepository.findByKakaoId(kakaoUserInfo.getId()))
                .thenReturn(Optional.empty());

        // 4. 신규 유저 저장 시 JPA가 ID를 할당하는 동작을 모방 (리플렉션 사용)
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
            return user;
        });

        // 5. JWT 토큰 생성 모킹 (JwTokenDto 생성자: grantType, accessToken, refreshToken, accessTokenExpiresIn)
        JwTokenDto dummyToken = new JwTokenDto(
                "Bearer",
                "access-token",
                "refresh-token",
                new Date(System.currentTimeMillis() + 3600 * 1000)
        );
        when(jwtTokenProvider.generateToken(anyString())).thenReturn(dummyToken);

        // 6. 로그인 메소드 호출
        UserLoginResponse loginResponse = userService.login(code);

        // 7. 결과 검증
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isEqualTo("access-token");
        assertThat(loginResponse.getRefreshToken()).isEqualTo("refresh-token");

        // 8. 모킹된 메소드 호출 여부 검증
        verify(restTemplate).postForEntity(
                eq("https://kauth.kakao.com/oauth/token"),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        );
        verify(restTemplate).exchange(
                eq("https://kapi.kakao.com/v2/user/me"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(KakaoUserInfo.class)
        );
        verify(userRepository).findByKakaoId(kakaoUserInfo.getId());
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateToken(anyString());
    }

    /**
     * Kakao 로그인 기능 테스트 - 기존 유저인 경우
     */
    @Test
    void testLogin_ExistingUser() throws Exception {
        String code = "dummyCode";

        // 1. Kakao Access Token 응답 모킹
        KakaoTokenResponse tokenResponse = KakaoTokenResponse.builder()
                .access_token("dummyAccessToken")
                .token_type("bearer")
                .refresh_token("dummyRefreshToken")
                .expires_in("3600")
                .scope("profile")
                .refresh_token_expires_in("86400")
                .build();
        ResponseEntity<KakaoTokenResponse> tokenResponseEntity =
                new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(
                eq("https://kauth.kakao.com/oauth/token"),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenReturn(tokenResponseEntity);

        // 2. Kakao User Info 응답 모킹
        KakaoUserInfo.KakaoAccount.Profile profile = KakaoUserInfo.KakaoAccount.Profile.builder()
                .nickname("TestUser")
                .build();

        KakaoUserInfo.KakaoAccount account = KakaoUserInfo.KakaoAccount.builder()
                .profile(profile)
                .build();

        KakaoUserInfo kakaoUserInfo = KakaoUserInfo.builder()
                .id(12345L)
                .kakaoAccount(account)
                .build();

        // 카카오 사용자 정보 응답 모킹 추가
        ResponseEntity<KakaoUserInfo> userInfoResponseEntity =
                new ResponseEntity<>(kakaoUserInfo, HttpStatus.OK);
        when(restTemplate.exchange(
                eq("https://kapi.kakao.com/v2/user/me"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(KakaoUserInfo.class)
        )).thenReturn(userInfoResponseEntity);

        // 3. 기존 유저가 존재함을 시뮬레이션
        User existingUser = User.builder()
                .kakaoId(kakaoUserInfo.getId())
                .nickname(kakaoUserInfo.getKakaoAccount().getProfile().getNickname())
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(existingUser, 1L);

        // findByKakaoId 모킹 추가
        when(userRepository.findByKakaoId(kakaoUserInfo.getId()))
                .thenReturn(Optional.of(existingUser));


        JwTokenDto dummyToken = new JwTokenDto(
                "Bearer",
                "existing-access-token",
                "existing-refresh-token",
                new Date(System.currentTimeMillis() + 3600 * 1000)
        );
        when(jwtTokenProvider.generateToken(anyString())).thenReturn(dummyToken);

        UserLoginResponse loginResponse = userService.login(code);

        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isEqualTo("existing-access-token");
        assertThat(loginResponse.getRefreshToken()).isEqualTo("existing-refresh-token");

        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider).generateToken(anyString());
    }

    /**
     * 로그아웃 기능 테스트
     */
    @Test
    void testLogout() {
        // SecurityContextHolder에 임의의 Authentication 설정
        Authentication auth = new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 로그아웃 호출
        userService.logout();

        // SecurityContextHolder의 Authentication이 클리어 되었는지 확인
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 내 정보 조회 기능 테스트
     */
    @Test
    void testGetMyInfo() throws Exception {
        // SecurityContextHolder에 인증정보 설정 (사용자 ID: "1")
        Authentication auth = new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 테스트용 User 객체 생성
        User user = User.builder()
                .kakaoId(12345L)
                .nickname("TestUser")
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);

        // BaseEntity에 정의된 createdAt 필드에 현재 날짜를 설정 (LocalDateTime 타입 사용)
        Field createdAtField = user.getClass().getSuperclass().getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(user, LocalDateTime.now());

        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));

        // 내 정보 조회 메소드 호출
        UserResponse userResponse = userService.getMyInfo();

        // 반환된 UserResponse 검증
        assertThat(userResponse).isNotNull();
        assertThat(userResponse.getId()).isEqualTo(1L);
        assertThat(userResponse.getNickname()).isEqualTo("TestUser");
    }

    /**
     * 탈퇴 기능 테스트
     */
    @Test
    void testWithdraw() throws Exception {
        // SecurityContextHolder에 인증정보 설정 (사용자 ID: "1")
        Authentication auth = new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 탈퇴 전 상태의 User 객체 생성
        User user = User.builder()
                .kakaoId(12345L)
                .nickname("TestUser")
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);

        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));

        // 탈퇴 메소드 호출
        userService.withdraw();

        // 사용자 상태가 탈퇴 처리되었는지 확인
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
    }
}
