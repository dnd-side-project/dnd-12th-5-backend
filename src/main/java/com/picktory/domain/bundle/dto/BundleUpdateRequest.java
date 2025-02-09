package com.picktory.domain.bundle.dto;

import com.picktory.domain.gift.dto.GiftUpdateRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BundleUpdateRequest {

    @NotNull
    private Long bundleId;

    @NotEmpty(message = "보따리에 포함될 선물 리스트는 최소 2개 이상이어야 합니다.")
    private List<GiftUpdateRequest> gifts;
}
