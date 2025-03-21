package com.picktory.domain.refreshToken.service;

import com.picktory.domain.refreshToken.dto.RefreshTokenResponse;
import com.picktory.domain.refreshToken.entity.RefreshToken;
import com.picktory.domain.refreshToken.repository.RefreshTokenRepository;
import com.picktory.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 리프레시 토큰 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * 새로운 리프레시 토큰을 생성하거나 기존 토큰을 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param token 리프레시 토큰 문자열
     * @param expiryDate 토큰 만료 일시
     * @return 저장된 리프레시 토큰 엔티티
     */
    @Transactional
    public RefreshToken createRefreshToken(String userId, String token, LocalDateTime expiryDate) {
        // 사용자 존재 여부 확인
        userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        log.debug("Creating or updating refresh token for user: {}", userId);

        // 기존 토큰이 있으면 업데이트, 없으면 새로 생성
        return refreshTokenRepository.findByUserId(userId)
                .map(existingToken -> {
                    log.debug("Updating existing refresh token for user: {}", userId);
                    existingToken.updateToken(token, expiryDate);
                    return existingToken;
                })
                .orElseGet(() -> {
                    log.debug("Creating new refresh token for user: {}", userId);
                    RefreshToken refreshToken = RefreshToken.create(userId, token, expiryDate);
                    return refreshTokenRepository.save(refreshToken);
                });
    }

    /**
     * 토큰 문자열로 리프레시 토큰을 조회합니다.
     *
     * @param token 리프레시 토큰 문자열
     * @return 리프레시 토큰 엔티티 (Optional)
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * 사용자 ID로 리프레시 토큰을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 리프레시 토큰 엔티티 (Optional)
     */
    public Optional<RefreshToken> findByUserId(String userId) {
        return refreshTokenRepository.findByUserId(userId);
    }

    /**
     * 리프레시 토큰의 만료 여부를 확인합니다.
     * 만료된 경우 예외를 발생시킵니다.
     *
     * @param token 확인할 리프레시 토큰 엔티티
     * @return 유효한 리프레시 토큰 엔티티
     * @throws IllegalStateException 토큰이 만료된 경우
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new IllegalStateException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    /**
     * 사용자 ID로 리프레시 토큰을 삭제합니다.
     *
     * @param userId 사용자 ID
     */
    @Transactional
    public void deleteByUserId(String userId) {
        log.debug("Deleting refresh token for user: {}", userId);
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * 토큰 정보를 DTO로 변환합니다.
     *
     * @param refreshToken 리프레시 토큰 엔티티
     * @return 리프레시 토큰 응답 DTO
     */
    public RefreshTokenResponse toDto(RefreshToken refreshToken) {
        return RefreshTokenResponse.of(
                refreshToken.getUserId(),
                refreshToken.getToken(),
                refreshToken.getExpiryDate()
        );
    }
}