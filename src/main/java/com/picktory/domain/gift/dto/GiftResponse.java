package com.picktory.domain.gift.dto;

import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.enums.GiftResponseTag;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
@Builder
public class GiftResponse {
    private Long id;
    private String name;
    private String message;
    private String purchaseUrl;
    private GiftResponseTag responseTag;
    private Boolean isResponsed;
    private List<String> imageUrls;

    public static GiftResponse fromEntity(Gift gift, List<GiftImage> images) {
        if (images == null || images.isEmpty()) {
            return GiftResponse.builder()
                    .id(gift.getId())
                    .name(gift.getName())
                    .message(gift.getMessage())
                    .purchaseUrl(gift.getPurchaseUrl())
                    .responseTag(gift.getResponseTag())
                    .isResponsed(gift.getIsResponsed())
                    .imageUrls(Collections.emptyList()) // 이미지가 없는 경우 빈 리스트 반환
                    .build();
        }

        // `isPrimary = true`인 이미지 찾기, 없으면 첫 번째 이미지 사용
        GiftImage primaryImage = images.stream()
                .filter(GiftImage::getIsPrimary)
                .findFirst()
                .orElse(images.get(0));

        // imageUrls 리스트 구성 (썸네일을 리스트 첫 번째로 설정)
        List<String> imageUrls = new ArrayList<>();
        imageUrls.add(primaryImage.getImageUrl()); // 대표 이미지 추가
        images.stream()
                .map(GiftImage::getImageUrl)
                .filter(url -> !url.equals(primaryImage.getImageUrl())) // 이미 추가된 대표 이미지는 제외
                .forEach(imageUrls::add);

        return GiftResponse.builder()
                .id(gift.getId())
                .name(gift.getName())
                .message(gift.getMessage())
                .purchaseUrl(gift.getPurchaseUrl())
                .responseTag(gift.getResponseTag())
                .isResponsed(gift.getIsResponsed())
                .imageUrls(imageUrls) // 대표 이미지가 항상 첫 번째
                .build();
    }
}