package com.picktory.domain.response.dto;

import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
public class ResponseResultDto {
    private Long id;
    private List<GiftResultInfo> gifts;

    @Getter
    @Builder
    public static class GiftResultInfo {
        private Long id;
        private String name;
        private String thumbnail;
        private String purchaseUrl;
        private String responseTag;
    }

    public static ResponseResultDto fromEntities(Long bundleId, List<Gift> gifts,
                                                 List<GiftImage> images) {
        // 선물 ID별 이미지 맵 생성 (주요 이미지 찾기 위함)
        Map<Long, List<GiftImage>> giftImagesMap = images.stream()
                .collect(Collectors.groupingBy(GiftImage::getGiftId));

        List<GiftResultInfo> giftInfos = gifts.stream()
                .map(gift -> {
                    // 선물에 대한 대표 이미지(썸네일) 찾기
                    String thumbnail = null;
                    if (giftImagesMap.containsKey(gift.getId())) {
                        // isPrimary가 true인 이미지를 우선적으로 찾음
                        thumbnail = giftImagesMap.get(gift.getId()).stream()
                                .filter(GiftImage::getIsPrimary)
                                .findFirst()
                                .map(GiftImage::getImageUrl)
                                .orElseGet(() -> giftImagesMap.get(gift.getId()).isEmpty() ?
                                        null : giftImagesMap.get(gift.getId()).get(0).getImageUrl());
                    }

                    return GiftResultInfo.builder()
                            .id(gift.getId())
                            .name(gift.getName())
                            .thumbnail(thumbnail)
                            .purchaseUrl(gift.getPurchaseUrl())
                            .responseTag(gift.getResponseTag() != null ?
                                    gift.getResponseTag().name() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseResultDto.builder()
                .id(bundleId)
                .gifts(giftInfos)
                .build();
    }
}