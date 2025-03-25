package com.picktory.domain.user.service;

import com.picktory.common.exception.BaseException;
import com.picktory.common.BaseResponseStatus;
import com.picktory.domain.auth.refresh.service.RefreshTokenService;
import com.picktory.domain.user.dto.UserResponse;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
import com.picktory.domain.auth.oauth.client.KakaoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final KakaoClient kakaoClient;
    private final RefreshTokenService refreshTokenService;

    /**
     * 현재 인증된 사용자의 정보를 조회합니다.
     *
     * @return 사용자 응답 DTO
     * @throws BaseException 인증된 사용자가 없거나 조회할 수 없는 경우
     */
    public UserResponse getMyInfo() {
        User user = getCurrentActiveUser();
        log.debug("Retrieved user info: userId={}", user.getId());
        return UserResponse.from(user);
    }

    /**
     * 사용자 계정을 탈퇴 처리합니다.
     * 카카오 계정 연결 해제 및 리프레시 토큰 삭제를 포함합니다.
     *
     * @throws BaseException 처리 중 오류 발생 시
     */
    @Transactional
    public void withdraw() {
        User user = getCurrentActiveUser();
        log.info("Processing withdrawal for user: {}", user.getId());

        try {
            // 1. 카카오 계정 연결 해제
            kakaoClient.unlinkKakaoAccount(user.getKakaoId());

            // 2. 리프레시 토큰 삭제
            refreshTokenService.deleteByUserId(user.getId());

            // 3. 사용자 삭제 처리
            user.delete();

            log.info("User successfully withdrawn: {}", user.getId());

        } catch (Exception e) {
            log.error("Failed to withdraw user: {}", user.getId(), e);
            throw new BaseException(BaseResponseStatus.KAKAO_API_ERROR);
        }
    }

    /**
     * 사용자 로그아웃을 처리합니다.
     * 보안 컨텍스트를 초기화합니다.
     */
    @Transactional
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String userId = authentication.getName();
            log.info("User logged out: {}", userId);

            // 리프레시 토큰 삭제
            refreshTokenService.deleteByUserId(Long.parseLong(userId));
        }

        // 보안 컨텍스트 초기화
        SecurityContextHolder.clearContext();
    }

    /**
     * 현재 인증된 활성 사용자를 조회합니다.
     *
     * @return 사용자 엔티티
     * @throws BaseException 인증된 사용자가 없거나 조회할 수 없는 경우
     */
    private User getCurrentActiveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found");
            throw new BaseException(BaseResponseStatus.INVALID_JWT);
        }

        try {
            Long userId = Long.parseLong(authentication.getName());
            return userRepository.findByIdAndIsDeletedFalse(userId)
                    .orElseThrow(() -> {
                        log.warn("User not found or deleted: {}", userId);
                        return new BaseException(BaseResponseStatus.USER_NOT_FOUND);
                    });
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", authentication.getName());
            throw new BaseException(BaseResponseStatus.INVALID_JWT);
        }
    }

    /**
     * ID로 사용자를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 사용자 엔티티
     * @throws BaseException 사용자를 찾을 수 없는 경우
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new BaseException(BaseResponseStatus.USER_NOT_FOUND);
                });
    }

    /**
     * ID로 활성 사용자를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 사용자 엔티티
     * @throws BaseException 사용자를 찾을 수 없거나 삭제된 경우
     */
    public User getActiveUserById(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    log.warn("Active user not found: {}", userId);
                    return new BaseException(BaseResponseStatus.USER_NOT_FOUND);
                });
    }

    /**
     * 카카오 ID로 사용자를 조회합니다.
     *
     * @param kakaoId 카카오 사용자 ID
     * @return 사용자 엔티티
     * @throws BaseException 사용자를 찾을 수 없는 경우
     */
    public User getUserByKakaoId(Long kakaoId) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> {
                    log.warn("User not found with Kakao ID: {}", kakaoId);
                    return new BaseException(BaseResponseStatus.USER_NOT_FOUND);
                });
    }
}