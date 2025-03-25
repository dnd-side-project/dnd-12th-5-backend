package com.picktory.config.auth;

import com.picktory.common.exception.BaseException;
import com.picktory.common.BaseResponseStatus;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
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
            throw new BaseException(BaseResponseStatus.INVALID_JWT);
        }

        // JWT의 subject 값은 서비스의 userId이므로, 이를 숫자로 변환하여 조회
        String authName = authentication.getName();
        System.out.println("인증된 사용자 ID: " + authName);

        try {
            Long userId = Long.parseLong(authName);
            return userRepository.findByIdAndIsDeletedFalse(userId)
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));
        } catch (NumberFormatException e) {
            throw new BaseException(BaseResponseStatus.INVALID_USER_ID);
        }
    }
}