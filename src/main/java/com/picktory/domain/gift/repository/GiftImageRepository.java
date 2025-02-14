package com.picktory.domain.gift.repository;

import com.picktory.domain.gift.entity.GiftImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftImageRepository extends JpaRepository<GiftImage, Long> {
    List<GiftImage> findByGiftId(Long giftId);
    List<GiftImage> findByGiftIdIn(List<Long> giftIds);
}
