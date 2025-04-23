package com.picktory.auth.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.domain.auth.dto.TokenDto;
import com.picktory.domain.auth.jwt.JwtTokenProvider;
import com.picktory.domain.auth.refresh.entity.RefreshToken;
import com.picktory.domain.auth.refresh.service.RefreshTokenService;
import com.picktory.domain.auth.service.AuthService;
import com.picktory.domain.user.dto.UserLoginResponse;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RefreshToken validRefreshToken;
    private TokenDto newTokenDto;
    private String refreshTokenStr;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 설정
        testUser = User.builder()
                .id(1L)
                .kakaoId(12345L)
                .nickname("테스트 사용자")
                .build();

        // 리프레시 토큰 설정
        refreshTokenStr = "valid-refresh-token";
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
        validRefreshToken = RefreshToken.builder()
                .id(1L)
                .userId(testUser.getId())
                .token(refreshTokenStr)
                .expiryDate(expiryDate)
                .build();

        // 새 토큰 DTO 설정
        Date accessTokenExpiresIn = Date.from(
                LocalDateTime.now().plusHours(24).atZone(ZoneId.systemDefault()).toInstant()
        );

        Date refreshTokenExpiresIn = Date.from(
                LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant()
        );

        newTokenDto = TokenDto.builder()
                .grantType("Bearer ")
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .refreshTokenExpiresIn(refreshTokenExpiresIn)
                .build();
    }

    @Test
    @DisplayName("리프레시 토큰 갱신 성공 테스트")
    void refreshTokenSuccess() {
        // 준비 - 명확한 매개변수 매칭을 위해 eq() 사용
        when(jwtTokenProvider.validateToken(eq(refreshTokenStr))).thenReturn(true);
        when(refreshTokenService.findByToken(eq(refreshTokenStr))).thenReturn(Optional.of(validRefreshToken));
        when(refreshTokenService.verifyExpiration(eq(validRefreshToken))).thenReturn(validRefreshToken);
        when(userRepository.findById(eq(testUser.getId()))).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(eq(testUser.getId()))).thenReturn(newTokenDto);

        // RefreshTokenService.createRefreshToken이 RefreshToken을 반환하는 경우
        RefreshToken newRefreshToken = RefreshToken.builder()
                .id(2L)
                .userId(testUser.getId())
                .token(newTokenDto.getRefreshToken())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        // 명확한 매개변수 지정
        LocalDateTime expectedExpiryDate = newTokenDto.getRefreshTokenExpiresIn()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        when(refreshTokenService.createRefreshToken(
                eq(testUser.getId()),
                eq(newTokenDto.getRefreshToken()),
                eq(expectedExpiryDate)
        )).thenReturn(newRefreshToken);

        // 실행
        UserLoginResponse response = authService.refreshToken(refreshTokenStr);

        // 검증
        assertNotNull(response);
        assertEquals(newTokenDto.getAccessToken(), response.getAccessToken());
        assertEquals(newTokenDto.getRefreshToken(), response.getRefreshToken());

        verify(jwtTokenProvider).validateToken(eq(refreshTokenStr));
        verify(refreshTokenService).findByToken(eq(refreshTokenStr));
        verify(refreshTokenService).verifyExpiration(eq(validRefreshToken));
        verify(userRepository).findById(eq(testUser.getId()));
        verify(jwtTokenProvider).generateToken(eq(testUser.getId()));
        verify(refreshTokenService).createRefreshToken(
                eq(testUser.getId()),
                eq(newTokenDto.getRefreshToken()),
                eq(expectedExpiryDate)
        );
    }

    @Test
    @DisplayName("유효하지 않은 JWT 토큰으로 갱신 실패")
    void refreshTokenWithInvalidJwt() {
        // 준비 - 명확한 매개변수 매칭
        when(jwtTokenProvider.validateToken(eq(refreshTokenStr))).thenReturn(false);

        // 실행 & 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                authService.refreshToken(refreshTokenStr));

        assertEquals(BaseResponseStatus.INVALID_REFRESH_TOKEN, exception.getStatus());
        verify(jwtTokenProvider).validateToken(eq(refreshTokenStr));
        verify(refreshTokenService, never()).findByToken(anyString());
    }

    @Test
    @DisplayName("DB에 없는 리프레시 토큰으로 갱신 실패")
    void refreshTokenNotFoundInDb() {
        // 준비 - 명확한 매개변수 매칭
        when(jwtTokenProvider.validateToken(eq(refreshTokenStr))).thenReturn(true);
        when(refreshTokenService.findByToken(eq(refreshTokenStr))).thenReturn(Optional.empty());

        // 실행 & 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                authService.refreshToken(refreshTokenStr));

        assertEquals(BaseResponseStatus.INVALID_REFRESH_TOKEN, exception.getStatus());
        verify(jwtTokenProvider).validateToken(eq(refreshTokenStr));
        verify(refreshTokenService).findByToken(eq(refreshTokenStr));
    }
}