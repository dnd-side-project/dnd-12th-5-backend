package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.enums.DeliveryCharacterType;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.gift.dto.GiftRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BundleRequest {

    @NotNull
    private String name;

    @NotNull
    private DesignType designType;

    @NotNull
    @Size(min = 2)
    private List<GiftRequest> gifts;
}
