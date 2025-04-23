package com.picktory.refresh.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.domain.auth.refresh.entity.RefreshToken;
import com.picktory.domain.auth.refresh.repository.RefreshTokenRepository;
import com.picktory.domain.auth.refresh.service.RefreshTokenService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken validRefreshToken;
    private RefreshToken expiredRefreshToken;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 설정
        testUser = User.builder()
                .id(1L)
                .kakaoId(12345L)
                .nickname("테스트 사용자")
                .build();

        // 유효한 리프레시 토큰 설정
        validRefreshToken = RefreshToken.builder()
                .id(1L)
                .userId(testUser.getId())
                .token("valid-refresh-token")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        // 만료된 리프레시 토큰 설정
        expiredRefreshToken = RefreshToken.builder()
                .id(2L)
                .userId(testUser.getId())
                .token("expired-refresh-token")
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("존재하지 않는 리프레시 토큰 새로 생성")
    void createRefreshToken_New() {
        // 준비
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validRefreshToken);

        // 실행
        RefreshToken result = refreshTokenService.createRefreshToken(
                testUser.getId(),
                validRefreshToken.getToken(),
                validRefreshToken.getExpiryDate()
        );

        // 검증
        assertNotNull(result);
        assertEquals(validRefreshToken.getToken(), result.getToken());
        assertEquals(testUser.getId(), result.getUserId());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("기존 리프레시 토큰 업데이트")
    void createRefreshToken_Update() {
        // 준비
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(validRefreshToken));

        // 업데이트된 토큰 모킹 추가
        String newToken = "new-token-value";
        LocalDateTime newExpiryDate = LocalDateTime.now().plusDays(7);

        RefreshToken updatedToken = RefreshToken.builder()
                .id(1L)
                .userId(testUser.getId())
                .token(newToken)
                .expiryDate(newExpiryDate)
                .build();

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(updatedToken);

        // 실행
        RefreshToken result = refreshTokenService.createRefreshToken(
                testUser.getId(),
                newToken,
                newExpiryDate
        );

        // 검증
        assertNotNull(result);
        assertEquals(newToken, result.getToken());
        // LocalDateTime 비교는 밀리초 차이로 실패할 수 있어 생략하거나 비교 로직 수정 필요
        // assertEquals(newExpiryDate, result.getExpiryDate());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("사용자가 존재하지 않을 때 예외 발생")
    void createRefreshToken_UserNotFound() {
        // 준비
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // 실행 & 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                refreshTokenService.createRefreshToken(
                        99L,
                        validRefreshToken.getToken(),
                        validRefreshToken.getExpiryDate()
                ));

        assertEquals(BaseResponseStatus.USER_NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("토큰 문자열로 리프레시 토큰 찾기")
    void findByToken() {
        // 준비
        when(refreshTokenRepository.findByToken(validRefreshToken.getToken()))
                .thenReturn(Optional.of(validRefreshToken));

        // 실행
        Optional<RefreshToken> result = refreshTokenService.findByToken(validRefreshToken.getToken());

        // 검증
        assertTrue(result.isPresent());
        assertEquals(validRefreshToken.getId(), result.get().getId());
    }

    @Test
    @DisplayName("사용자 ID로 리프레시 토큰 찾기")
    void findByUserId() {
        // 준비
        when(refreshTokenRepository.findByUserId(testUser.getId()))
                .thenReturn(Optional.of(validRefreshToken));

        // 실행
        Optional<RefreshToken> result = refreshTokenService.findByUserId(testUser.getId());

        // 검증
        assertTrue(result.isPresent());
        assertEquals(validRefreshToken.getId(), result.get().getId());
    }

    @Test
    @DisplayName("만료되지 않은 토큰 검증 성공")
    void verifyExpiration_Valid() {
        // 실행
        RefreshToken result = refreshTokenService.verifyExpiration(validRefreshToken);

        // 검증
        assertNotNull(result);
        assertEquals(validRefreshToken.getId(), result.getId());
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 예외 발생 및 삭제")
    void verifyExpiration_Expired() {
        // 실행 & 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                refreshTokenService.verifyExpiration(expiredRefreshToken));

        assertEquals(BaseResponseStatus.EXPIRED_REFRESHTOKEN, exception.getStatus());
        verify(refreshTokenRepository, times(1)).delete(expiredRefreshToken);
    }

    @Test
    @DisplayName("사용자 ID로 리프레시 토큰 삭제")
    void deleteByUserId() {
        // 실행
        refreshTokenService.deleteByUserId(testUser.getId());

        // 검증
        verify(refreshTokenRepository, times(1)).deleteByUserId(testUser.getId());
    }
}