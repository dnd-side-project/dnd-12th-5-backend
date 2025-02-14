package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.DesignType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class BundleMainListResponse {
    private Long id;
    private String name;
    private DesignType designType;
    private LocalDateTime updatedAt;

    public static BundleMainListResponse fromEntity(Bundle bundle) {
        return BundleMainListResponse.builder()
                .id(bundle.getId())
                .name(bundle.getName())
                .designType(bundle.getDesignType())
                .updatedAt(bundle.getUpdatedAt())
                .build();
    }

    public static List<BundleMainListResponse> fromEntityList(List<Bundle> bundles) {
        return bundles.stream().map(BundleMainListResponse::fromEntity).collect(Collectors.toList());
    }
}
