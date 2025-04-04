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
public class BundleListResponse extends BundleDto {
    public BundleListResponse(Bundle bundle) {
        super(bundle);
    }

    public static List<BundleListResponse> listFrom(List<Bundle> bundles) {
        return bundles.stream().map(BundleListResponse::new).collect(Collectors.toList());
    }
}
