package com.picktory.domain.refreshToken.repository;

import com.picktory.domain.refreshToken.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 값으로 리프레시 토큰을 조회합니다.
     *
     * @param token 리프레시 토큰 문자열
     * @return 리프레시 토큰 엔티티
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 ID로 리프레시 토큰을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 리프레시 토큰 엔티티
     */
    Optional<RefreshToken> findByUserId(String userId);

    /**
     * 사용자 ID로 리프레시 토큰이 존재하는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    boolean existsByUserId(String userId);

    /**
     * 사용자 ID로 리프레시 토큰을 삭제합니다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(String userId);
}