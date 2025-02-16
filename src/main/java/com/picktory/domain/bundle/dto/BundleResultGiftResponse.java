package com.picktory.domain.bundle.dto;

import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.enums.GiftResponseTag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BundleResultGiftResponse {
    private Long id;
    private String name;
    private String purchaseUrl;
    private String thumbnail; // 대표 이미지 URL
    private GiftResponseTag responseTag;

    public static BundleResultGiftResponse from(Gift gift, GiftImage primaryImage) {
        return BundleResultGiftResponse.builder()
                .id(gift.getId())
                .name(gift.getName())
                .purchaseUrl(gift.getPurchaseUrl())
                .responseTag(gift.getResponseTag())
                .thumbnail(primaryImage != null ? primaryImage.getImageUrl() : null) // 대표 이미지가 없으면 null
                .build();
    }
}
