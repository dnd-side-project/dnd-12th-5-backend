package com.picktory.domain.response.dto;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ResponseBundleDto {
    private BundleInfo bundle;

    @Getter
    @Builder
    public static class BundleInfo {
        private String delivery_character_type;
        private String status;
        private String design_type;
        private List<GiftInfo> gifts;
        private int total_gifts;
    }

    @Getter
    @Builder
    public static class GiftInfo {
        private Long id;
        private String message;
        private List<String> imageUrls;
        private String thumbnail;
    }

    public static ResponseBundleDto fromEntity(Bundle bundle, List<Gift> gifts, List<GiftImage> images) {
        List<GiftInfo> giftInfos = gifts.stream()
                .map(gift -> {
                    List<GiftImage> giftImages = images.stream()
                            .filter(img -> img.getGiftId().equals(gift.getId()))
                            .collect(Collectors.toList());

                    return GiftInfo.builder()
                            .id(gift.getId())
                            .message(null) // 선물이 열리기 전에는 null
                            .imageUrls(giftImages.stream()
                                    .filter(img -> !img.getIsPrimary())
                                    .map(GiftImage::getImageUrl)
                                    .collect(Collectors.toList()))
                            .thumbnail(giftImages.stream()
                                    .filter(GiftImage::getIsPrimary)
                                    .findFirst()
                                    .map(GiftImage::getImageUrl)
                                    .orElse(null))
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseBundleDto.builder()
                .bundle(BundleInfo.builder()
                        .delivery_character_type(bundle.getDeliveryCharacterType().name())
                        .design_type(bundle.getDesignType().name())
                        .status(bundle.getStatus().name())
                        .gifts(giftInfos)
                        .total_gifts(gifts.size())
                        .build())
                .build();
    }
}