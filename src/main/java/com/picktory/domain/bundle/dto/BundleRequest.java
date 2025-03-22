package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DeliveryCharacterType;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.gift.dto.GiftRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BundleRequest {

    @NotNull
    private String name;

    @NotNull
    private DesignType designType;

    @NotNull
    @Size(min = 2)
    private List<GiftRequest> gifts;

    public Bundle toEntity(Long userId) {
        return Bundle.builder()
                .userId(userId)
                .name(this.name)
                .designType(this.designType)
                .status(BundleStatus.DRAFT)
                .isRead(false)
                .build();
    }
}
