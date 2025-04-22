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
        private Long id;
        private String deliveryCharacterType;
        private String status;
        private String designType;
        private List<GiftInfo> gifts;
        private int totalGifts;
    }

    @Getter
    @Builder
    public static class GiftInfo {
        private Long id;
        private String message;
        private List<String> imageUrls;
        private String name;
        private String thumbnail;
        private String responseTag;
    }

    public static ResponseBundleDto fromEntity(Bundle bundle, List<Gift> gifts, List<GiftImage> images) {
        List<GiftInfo> giftInfos = gifts.stream()
                .map(gift -> {
                    // 해당 선물의 이미지들 찾기
                    List<GiftImage> giftImages = images.stream()
                            .filter(img -> img.getGiftId().equals(gift.getId()))
                            .collect(Collectors.toList());

                    // 이미지가 있는 경우 첫 번째 이미지를 썸네일로 사용
                    String thumbnail = giftImages.isEmpty() ? null :
                            giftImages.get(0).getImageUrl();

                    return GiftInfo.builder()
                            .id(gift.getId())
                            .name(gift.getName())
                            .message(gift.getMessage())
                            .imageUrls(giftImages.stream()
                                    .map(GiftImage::getImageUrl)
                                    .collect(Collectors.toList()))
                            .thumbnail(thumbnail)
                            .responseTag(gift.getResponseTag() != null ? gift.getResponseTag().name() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseBundleDto.builder()
                .bundle(BundleInfo.builder()
                        .id(bundle.getId())
                        .deliveryCharacterType(bundle.getDeliveryCharacterType().name())
                        .designType(bundle.getDesignType().name())
                        .status(bundle.getStatus().name())
                        .gifts(giftInfos)
                        .totalGifts(gifts.size())
                        .build())
                .build();
    }
}