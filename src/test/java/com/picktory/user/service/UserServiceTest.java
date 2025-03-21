package com.picktory.user.service;

import com.picktory.domain.refreshToken.service.RefreshTokenService;
import com.picktory.domain.user.dto.UserResponse;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
import com.picktory.domain.user.service.UserService;
import com.picktory.domain.user.service.auth.KakaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KakaoService kakaoService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

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

        // 리프레시 토큰 삭제 확인
        verify(refreshTokenService).deleteByUserId("1");
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

        // BaseEntity에 정의된 createdAt 필드에 현재 날짜를 설정
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

        // 카카오 계정 연결 해제 및 리프레시 토큰 삭제 확인
        verify(kakaoService).unlinkKakaoAccount(12345L);
        verify(refreshTokenService).deleteByUserId("1");
    }

    /**
     * ID로 사용자 조회 테스트
     */
    @Test
    void testGetUserById() throws Exception {
        // 테스트용 User 객체 생성
        User user = User.builder()
                .kakaoId(12345L)
                .nickname("TestUser")
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // ID로 사용자 조회
        User foundUser = userService.getUserById(1L);

        // 결과 검증
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(1L);
        assertThat(foundUser.getNickname()).isEqualTo("TestUser");
    }

    /**
     * ID로 활성 사용자 조회 테스트
     */
    @Test
    void testGetActiveUserById() throws Exception {
        // 테스트용 User 객체 생성
        User user = User.builder()
                .kakaoId(12345L)
                .nickname("TestUser")
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);

        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(user));

        // ID로 활성 사용자 조회
        User foundUser = userService.getActiveUserById(1L);

        // 결과 검증
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(1L);
        assertThat(foundUser.getNickname()).isEqualTo("TestUser");
    }

    /**
     * 카카오 ID로 사용자 조회 테스트
     */
    @Test
    void testGetUserByKakaoId() throws Exception {
        // 테스트용 User 객체 생성
        User user = User.builder()
                .kakaoId(12345L)
                .nickname("TestUser")
                .build();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);

        when(userRepository.findByKakaoId(12345L)).thenReturn(Optional.of(user));

        // 카카오 ID로 사용자 조회
        User foundUser = userService.getUserByKakaoId(12345L);

        // 결과 검증
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(1L);
        assertThat(foundUser.getKakaoId()).isEqualTo(12345L);
    }
}