package com.picktory.config.auth;

import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
import com.picktory.common.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;

    /**
     * 현재 로그인한 사용자 조회
     */
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(BaseResponseStatus.INVALID_JWT.getMessage());
        }

        // JWT의 subject 값은 서비스의 userId이므로, 이를 숫자로 변환하여 조회
        String authName = authentication.getName();
        System.out.println("인증된 사용자 ID: " + authName);

        try {
            Long userId = Long.parseLong(authName);
            return userRepository.findByIdAndIsDeletedFalse(userId)
                    .orElseThrow(() -> new IllegalStateException(BaseResponseStatus.USER_NOT_FOUND.getMessage()));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("유효하지 않은 사용자 ID입니다.");
        }
    }
}
