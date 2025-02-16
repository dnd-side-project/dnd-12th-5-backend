package com.picktory.domain.gift.repository;

import com.picktory.domain.gift.entity.Gift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GiftRepository extends JpaRepository<Gift, Long> {
    List<Gift> findByBundleId(Long bundleId);

    @Modifying
    @Query("DELETE FROM Gift g WHERE g.id IN :giftIds")
    void deleteByIds(@Param("giftIds") List<Long> giftIds);

    Optional<Gift> findByIdAndBundleId(Long id, Long bundleId);
}
