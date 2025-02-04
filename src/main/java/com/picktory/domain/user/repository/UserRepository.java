package com.picktory.domain.user.repository;

import com.picktory.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 삭제되지 않은 사용자 ID로 조회
    Optional<User> findByIdAndIsDeletedFalse(Long id);
    
    // 카카오 아이디로 사용자 조회
    Optional<User> findByKakaoId(String kakaoId);
}