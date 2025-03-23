package com.picktory.user.service;

import com.picktory.domain.auth.jwt.JwtTokenProvider;
import com.picktory.domain.auth.dto.TokenDto;
import com.picktory.domain.auth.oauth.dto.KakaoUserInfo;
import com.picktory.domain.auth.refresh.entity.RefreshToken;
import com.picktory.domain.auth.refresh.service.RefreshTokenService;
import com.picktory.domain.user.dto.UserLoginResponse;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
import com.picktory.domain.auth.service.AuthService;
import com.picktory.domain.auth.oauth.client.KakaoClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KakaoClient kakaoClient;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    /**
     * 카카오 로그인 기능 테스트 - 신규 유저인 경우
     */
    @Test
    void testLoginWithKakao_NewUser() throws Exception {
        // 테스트 데이터 설정
        String code = "dummyCode";
        String kakaoAccessToken = "dummyKakaoToken";

        // 카카오 사용자 정보 설정
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

        // 모킹 설정
        when(kakaoClient.getKakaoAccessToken(code)).thenReturn(kakaoAccessToken);
        when(kakaoClient.getKakaoUserInfo(kakaoAccessToken)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.getId())).thenReturn(Optional.empty());

        // 신규 유저 저장 시 JPA가 ID를 할당하는 동작을 모방 (리플렉션 사용)
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
            return user;
        });

        // JWT 토큰 생성 모킹
        TokenDto tokenDto = TokenDto.builder()
                .grantType("Bearer")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .accessTokenExpiresIn(new Date(System.currentTimeMillis() + 3600 * 1000))
                .build();
        when(jwtTokenProvider.generateToken(anyLong())).thenReturn(tokenDto);

        // 실행
        UserLoginResponse response = authService.loginWithKakao(code);

        // 검증
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        // 메소드 호출 검증
        verify(kakaoClient).getKakaoAccessToken(code);
        verify(kakaoClient).getKakaoUserInfo(kakaoAccessToken);
        verify(userRepository).findByKakaoId(kakaoUserInfo.getId());
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateToken(1L);
        verify(refreshTokenService).createRefreshToken(eq(1L), eq("refresh-token"), any(LocalDateTime.class));
    }

    /**
     * 카카오 로그인 기능 테스트 - 기존 유저인 경우
     */
    @Test
    void testLoginWithKakao_ExistingUser() throws Exception {
        // 테스트 데이터 설정
        String code = "dummyCode";
        String kakaoAccessToken = "dummyKakaoToken";

        // 카카오 사용자 정보 설정
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

        // 기존 유저 설정
        User existingUser = User.builder()
                .kakaoId(kakaoUserInfo.getId())
                .nickname(kakaoUserInfo.getKakaoAccount().getProfile().getNickname())
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(existingUser, 1L);

        // 모킹 설정
        when(kakaoClient.getKakaoAccessToken(code)).thenReturn(kakaoAccessToken);
        when(kakaoClient.getKakaoUserInfo(kakaoAccessToken)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(kakaoUserInfo.getId())).thenReturn(Optional.of(existingUser));

        // JWT 토큰 생성 모킹
        TokenDto tokenDto = TokenDto.builder()
                .grantType("Bearer")
                .accessToken("existing-access-token")
                .refreshToken("existing-refresh-token")
                .accessTokenExpiresIn(new Date(System.currentTimeMillis() + 3600 * 1000))
                .build();
        when(jwtTokenProvider.generateToken(1L)).thenReturn(tokenDto);

        // 실행
        UserLoginResponse response = authService.loginWithKakao(code);

        // 검증
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("existing-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("existing-refresh-token");

        // 메소드 호출 검증
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider).generateToken(1L);
        verify(refreshTokenService).createRefreshToken(eq(1L), eq("existing-refresh-token"), any(LocalDateTime.class));
    }

    /**
     * 토큰 갱신 테스트
     */
    @Test
    void testRefreshToken() throws Exception {
        // 테스트 데이터 설정
        String refreshTokenStr = "valid-refresh-token";
        Long userId = 1L;

        // 리프레시 토큰 엔티티 설정
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(userId)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        // 사용자 설정
        User user = User.builder()
                .kakaoId(12345L)
                .nickname("TestUser")
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);

        // 모킹 설정
        when(refreshTokenService.findByToken(refreshTokenStr)).thenReturn(Optional.of(refreshTokenEntity));
        when(refreshTokenService.verifyExpiration(refreshTokenEntity)).thenReturn(refreshTokenEntity);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // 새 토큰 생성 모킹
        TokenDto newTokenDto = TokenDto.builder()
                .grantType("Bearer")
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .accessTokenExpiresIn(new Date(System.currentTimeMillis() + 3600 * 1000))
                .build();
        when(jwtTokenProvider.generateToken(userId)).thenReturn(newTokenDto);

        // 실행
        UserLoginResponse response = authService.refreshToken(refreshTokenStr);

        // 검증
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");

        // 메소드 호출 검증
        verify(refreshTokenService).findByToken(refreshTokenStr);
        verify(refreshTokenService).verifyExpiration(refreshTokenEntity);
        verify(userRepository).findById(1L);
        verify(jwtTokenProvider).generateToken(userId);
        verify(refreshTokenService).createRefreshToken(eq(userId), eq("new-refresh-token"), any(LocalDateTime.class));
    }
}