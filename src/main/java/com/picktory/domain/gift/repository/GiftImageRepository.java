package com.picktory.domain.gift.repository;

import com.picktory.domain.gift.entity.GiftImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GiftImageRepository extends JpaRepository<GiftImage, Long> {

    List<GiftImage> findAllByGift_Id(Long giftId);

    List<GiftImage> findAllByGift_IdIn(List<Long> giftIds);

    void deleteAllByGift_IdIn(List<Long> giftIds);

    Optional<GiftImage> findByGift_IdAndIsPrimaryTrue(Long giftId);
}