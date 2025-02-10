package com.picktory.domain.bundle.repository;

import com.picktory.domain.bundle.entity.Bundle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface BundleRepository extends JpaRepository<Bundle, Long> {
    /**
     * 특정 사용자가 오늘 생성한 보따리 개수 조회
     */
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime today);
}
