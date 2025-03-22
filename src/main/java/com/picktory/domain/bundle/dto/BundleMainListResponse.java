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
public class BundleMainListResponse extends BundleDto {
    public BundleMainListResponse(Bundle bundle) {
        super(bundle);
        this.isRead = bundle.getStatus() == BundleStatus.COMPLETED && !bundle.getIsRead() ? false : true;
    }

    public static List<BundleMainListResponse> listFrom(List<Bundle> bundles) {
        return bundles.stream()
                .map(BundleMainListResponse::new)
                .collect(Collectors.toList());
    }
}

