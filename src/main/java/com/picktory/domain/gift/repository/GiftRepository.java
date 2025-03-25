package com.picktory.domain.gift.repository;

import com.picktory.domain.gift.entity.Gift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GiftRepository extends JpaRepository<Gift, Long> {

    List<Gift> findAllByBundleId(Long bundleId);

    Optional<Gift> findByIdAndBundleId(Long giftId, Long bundleId);
}
