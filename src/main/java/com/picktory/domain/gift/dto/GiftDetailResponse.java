package com.picktory.domain.gift.dto;

import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class GiftDetailResponse {
    private Long id;
    private String name;
    private String message;
    private String purchaseUrl;
    private String thumbnail;    // isPrimary = true인 이미지
    private List<String> imageUrls;  // isPrimary = false인 이미지들

    public static GiftDetailResponse fromEntity(Gift gift, List<GiftImage> images) {
        // 대표 이미지(썸네일) 찾기
        String thumbnail = images.stream()
                .filter(GiftImage::getIsPrimary)
                .findFirst()
                .map(GiftImage::getImageUrl)
                .orElse(null);

        // 나머지 이미지들
        List<String> imageUrls = images.stream()
                .filter(image -> !image.getIsPrimary())
                .map(GiftImage::getImageUrl)
                .collect(Collectors.toList());

        return GiftDetailResponse.builder()
                .id(gift.getId())
                .name(gift.getName())
                .message(gift.getMessage())
                .purchaseUrl(gift.getPurchaseUrl())
                .thumbnail(thumbnail)
                .imageUrls(imageUrls)
                .build();
    }
}