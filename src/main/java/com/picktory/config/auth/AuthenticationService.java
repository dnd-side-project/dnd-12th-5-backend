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

        Long currentUserId = Long.parseLong(authentication.getName());
        return userRepository.findByIdAndIsDeletedFalse(currentUserId)
                .orElseThrow(() -> new IllegalStateException(BaseResponseStatus.USER_NOT_FOUND.getMessage()));
    }
}
