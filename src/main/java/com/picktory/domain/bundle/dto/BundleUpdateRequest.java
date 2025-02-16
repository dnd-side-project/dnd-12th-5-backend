package com.picktory.domain.bundle.dto;

import com.picktory.domain.gift.dto.GiftUpdateRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BundleUpdateRequest {

    @NotNull
    private Long bundleId;

    @NotEmpty(message = "보따리에 포함될 선물 리스트는 최소 2개 이상이어야 합니다.")
    @Size(max = 6, message = "보따리에는 최대 6개의 선물만 포함될 수 있습니다.")
    private List<GiftUpdateRequest> gifts;
}
