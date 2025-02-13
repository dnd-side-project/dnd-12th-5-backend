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

        // 사용자의 ID가 숫자가 아니라면 Kakao ID로 조회
        String authName = authentication.getName();
        System.out.println("인증된 사용자 ID: " + authName);

        // 만약 `authName`이 숫자가 아닌 경우, Kakao OAuth로 로그인한 사용자로 가정
        return userRepository.findByKakaoId(authName)
                .orElseThrow(() -> new IllegalStateException(BaseResponseStatus.USER_NOT_FOUND.getMessage()));
    }

}
