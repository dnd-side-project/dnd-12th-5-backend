package com.picktory.domain.gift.dto;

import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
public class DraftGiftsResponse {
    private List<GiftDetailResponse> gifts;

    public static DraftGiftsResponse from(List<Gift> gifts, List<GiftImage> images) {
        // 선물 ID별로 이미지 그룹화
        Map<Long, List<GiftImage>> giftImagesMap = images.stream()
                .collect(Collectors.groupingBy(
                        image -> image.getGift().getId()
                ));

        // 각 선물에 대한 DetailResponse 생성
        List<GiftDetailResponse> giftResponses = gifts.stream()
                .map(gift -> {
                    // 해당 선물의 이미지 목록 가져오기
                    List<GiftImage> giftImages = giftImagesMap.getOrDefault(gift.getId(), Collections.emptyList());

                    // 대표 이미지(썸네일) 찾기
                    String thumbnail = giftImages.stream()
                            .filter(GiftImage::getIsPrimary)
                            .findFirst()
                            .map(GiftImage::getImageUrl)
                            .orElse(null);

                    // 나머지 이미지 URL 목록
                    List<String> imageUrls = giftImages.stream()
                            .filter(image -> !image.getIsPrimary())
                            .map(GiftImage::getImageUrl)
                            .collect(Collectors.toList());

                    // GiftDetailResponse 생성
                    return GiftDetailResponse.builder()
                            .id(gift.getId())
                            .name(gift.getName())
                            .message(gift.getMessage())
                            .purchaseUrl(gift.getPurchaseUrl())
                            .thumbnail(thumbnail)
                            .imageUrls(imageUrls)
                            .build();
                })
                .collect(Collectors.toList());

        return DraftGiftsResponse.builder()
                .gifts(giftResponses)
                .build();
    }
}