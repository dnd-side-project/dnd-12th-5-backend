package com.picktory.domain.gift.repository;

import com.picktory.domain.gift.entity.GiftImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GiftImageRepository extends JpaRepository<GiftImage, Long> {
//    List<GiftImage> findByGiftId(Long giftId);
    @Query("SELECT gi FROM GiftImage gi WHERE gi.gift.id = :giftId")
    List<GiftImage> findByGiftId(@Param("giftId") Long giftId);

//    List<GiftImage> findByGiftIdIn(List<Long> giftIds);
    // giftId 리스트로 조회 (JPQL 적용)
    @Query("SELECT gi FROM GiftImage gi WHERE gi.gift.id IN :giftIds")
    List<GiftImage> findByGiftIdIn(@Param("giftIds") List<Long> giftIds);

    @Modifying
    @Query("DELETE FROM GiftImage gi WHERE gi.gift.id IN :giftIds")
    void deleteByGiftIds(@Param("giftIds") List<Long> giftIds);

    // 특정 선물의 대표 이미지(`isPrimary = true`) 조회
    @Query("SELECT gi FROM GiftImage gi WHERE gi.gift.id = :giftId AND gi.isPrimary = true")
    Optional<GiftImage> findPrimaryImageByGiftId(@Param("giftId") Long giftId);

}
