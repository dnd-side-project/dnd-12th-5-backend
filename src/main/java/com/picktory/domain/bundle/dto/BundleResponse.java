package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DeliveryCharacterType;
import com.picktory.domain.bundle.enums.DesignType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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

    public static BundleResponse fromEntity(Bundle bundle) {
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
                .build();
    }
}
