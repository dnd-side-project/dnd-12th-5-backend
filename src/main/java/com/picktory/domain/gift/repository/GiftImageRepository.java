package com.picktory.domain.gift.repository;

import com.picktory.domain.gift.entity.GiftImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GiftImageRepository extends JpaRepository<GiftImage, Long> {
    List<GiftImage> findByGiftId(Long giftId);

    List<GiftImage> findByGiftIdIn(List<Long> giftIds);
}

    @Modifying
    @Query("DELETE FROM GiftImage gi WHERE gi.gift.id IN :giftIds")
    void deleteByGiftIds(@Param("giftIds") List<Long> giftIds);
}
