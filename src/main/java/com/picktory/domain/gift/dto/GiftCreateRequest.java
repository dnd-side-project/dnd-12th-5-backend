package com.picktory.domain.gift.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GiftCreateRequest {

    @NotNull
    private String name;

    private String message;
    private String purchaseUrl;
}
