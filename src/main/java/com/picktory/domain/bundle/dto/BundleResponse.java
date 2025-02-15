package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DeliveryCharacterType;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.gift.dto.GiftRequest;
import com.picktory.domain.gift.dto.GiftResponse;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class BundleResponse {

    private Long id;
    private Long userId;
    private String name;
    private DesignType designType;
    private DeliveryCharacterType deliveryCharacterType;
    private String link;
    private BundleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private Boolean isRead;
    private List<GiftResponse> gifts;

    public static BundleResponse fromEntity(Bundle bundle, List<Gift> gifts, List<GiftImage> images) {
        return BundleResponse.builder()
                .id(bundle.getId())
                .userId(bundle.getUserId())
                .name(bundle.getName())
                .designType(bundle.getDesignType())
                .deliveryCharacterType(bundle.getDeliveryCharacterType())
                .link(bundle.getLink())
                .status(bundle.getStatus())
                .createdAt(bundle.getCreatedAt())
                .updatedAt(bundle.getUpdatedAt())
                .publishedAt(bundle.getPublishedAt())
                .isRead(bundle.getIsRead())
                .gifts(gifts == null ? Collections.emptyList() :
                        gifts.stream()
                                .map(gift -> GiftResponse.fromEntity(
                                        gift, images.stream()
                                                .filter(image -> image.getGift() != null && image.getGift().getId().equals(gift.getId()))
                                                .collect(Collectors.toList())
                                ))
                                .collect(Collectors.toList()))
                .build();
    }

}
