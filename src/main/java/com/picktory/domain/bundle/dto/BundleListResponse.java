package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DesignType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class BundleListResponse {
    private Long id;
    private String name;
    private DesignType designType;
    private LocalDateTime updatedAt;
    private BundleStatus status;
    private Boolean isRead;

    public static BundleListResponse fromEntity(Bundle bundle) {
        return BundleListResponse.builder()
                .id(bundle.getId())
                .name(bundle.getName())
                .designType(bundle.getDesignType())
                .updatedAt(bundle.getUpdatedAt())
                .status(bundle.getStatus())
                .isRead(bundle.getIsRead())
                .build();
    }

    public static List<BundleListResponse> fromEntityList(List<Bundle> bundles) {
        return bundles.stream().map(BundleListResponse::fromEntity).collect(Collectors.toList());
    }
}
