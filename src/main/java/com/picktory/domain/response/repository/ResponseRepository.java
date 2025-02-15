package com.picktory.domain.response.repository;

import com.picktory.domain.response.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponseRepository extends JpaRepository<Response, Long>, ResponseRepositoryCustom {
    boolean existsByGiftId(Long giftId);
    boolean existsByGiftIdIn(List<Long> giftIds);
}