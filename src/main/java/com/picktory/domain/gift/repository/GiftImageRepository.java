package com.picktory.domain.gift.repository;

import com.picktory.domain.gift.entity.GiftImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GiftImageRepository extends JpaRepository<GiftImage, Long> {
    List<GiftImage> findByGiftId(Long giftId);

    @Modifying
    @Query("DELETE FROM GiftImage gi WHERE gi.giftId IN :giftIds")
    void deleteByGiftIds(@Param("giftIds") List<Long> giftIds);

    @Query("SELECT gi FROM GiftImage gi WHERE gi.giftId IN :giftIds")
    List<GiftImage> findByGiftIds(@Param("giftIds") List<Long> giftIds);

}
