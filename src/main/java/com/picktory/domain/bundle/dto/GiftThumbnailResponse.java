package com.picktory.domain.bundle.dto;

import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GiftThumbnailResponse {
    private Long id;
    private String thumbnail;

    public static GiftThumbnailResponse from(Gift gift, GiftImage primaryImage) {
        return GiftThumbnailResponse.builder()
                .id(gift.getId())
                .thumbnail(primaryImage != null ? primaryImage.getImageUrl() : null)
                .build();
    }
}
