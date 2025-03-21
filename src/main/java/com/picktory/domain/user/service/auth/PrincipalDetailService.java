package com.picktory.domain.user.service.auth;

import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.entity.auth.PrincipalDetails;
import com.picktory.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security에서 사용자 정보를 로드하기 위한 서비스
 * UserDetailsService 인터페이스를 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrincipalDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 사용자 ID로 사용자 정보를 로드
     *
     * @param userId 사용자 ID (문자열)
     * @return PrincipalDetails 객체 (UserDetails 구현체)
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Override
    @Transactional(readOnly = true)
    public PrincipalDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        try {
            Long id = Long.parseLong(userId);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

            log.debug("User loaded successfully: {}", userId);
            return new PrincipalDetails(user);
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userId);
            throw new UsernameNotFoundException("Invalid user ID format: " + userId);
        }
    }

    /**
     * 삭제되지 않은 활성 사용자 정보를 로드
     *
     * @param userId 사용자 ID (문자열)
     * @return PrincipalDetails 객체 (UserDetails 구현체)
     * @throws UsernameNotFoundException 사용자를 찾을 수 없거나 삭제된 경우
     */
    @Transactional(readOnly = true)
    public PrincipalDetails loadActiveUserByUsername(String userId) throws UsernameNotFoundException {
        try {
            Long id = Long.parseLong(userId);
            User user = userRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> new UsernameNotFoundException("Active user not found with id: " + userId));

            log.debug("Active user loaded successfully: {}", userId);
            return new PrincipalDetails(user);
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userId);
            throw new UsernameNotFoundException("Invalid user ID format: " + userId);
        }
    }
}