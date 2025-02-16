package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class BundleSummaryResponse {
    private Long id;
    private String name;
    private String designType;
    private String status;
    private String link;
    private List<GiftThumbnailResponse> gifts;

    public static BundleSummaryResponse fromEntity(Bundle bundle, List<Gift> gifts, List<GiftImage> images) {
        return BundleSummaryResponse.builder()
                .id(bundle.getId())
                .name(bundle.getName())
                .designType(bundle.getDesignType().name())
                .status(bundle.getStatus().name())
                .link(bundle.getStatus() == BundleStatus.DRAFT ? null : bundle.getLink())
                .gifts(gifts.stream()
                        .map(gift -> {
                            GiftImage primaryImage = images.stream()
                                    .filter(image -> image.getGift().getId().equals(gift.getId()) && image.getIsPrimary())
                                    .findFirst()
                                    .orElse(null);
                            return GiftThumbnailResponse.from(gift, primaryImage);
                        })
                        .collect(Collectors.toList()))
                .build();
    }
}
