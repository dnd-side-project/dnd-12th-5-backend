package com.picktory.domain.bundle.repository;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface BundleRepository extends JpaRepository<Bundle, Long> {
    /**
     * 특정 사용자가 오늘 생성한 보따리 개수 조회
     */
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime today);
    Optional<Bundle> findByLink(String link);
    Optional<Bundle> findByIdAndUserId(Long id, Long userId);

    /**
     * 특정 사용자의 보따리를 최신 업데이트순으로 조회
     */
    List<Bundle> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /**
     * 특정 사용자의 보따리를 최신 업데이트순으로 8개만 조회
     */
    @Query("SELECT b FROM Bundle b WHERE b.userId = :userId ORDER BY b.updatedAt DESC LIMIT 8")
    List<Bundle> findTop8ByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<Bundle> findByIdAndStatus(Long id, BundleStatus status);
}
