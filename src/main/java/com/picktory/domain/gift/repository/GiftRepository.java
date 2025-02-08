package com.picktory.domain.gift.repository;

import com.picktory.domain.gift.entity.Gift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftRepository extends JpaRepository<Gift, Long> {
    List<Gift> findByBundleId(Long bundleId);
}
