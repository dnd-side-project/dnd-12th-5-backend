package com.picktory.domain.gift.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftUpdateRequest implements GiftImageRequest {
    private Long id;

    @NotNull
    private String name;
    private String message;
    private String purchaseUrl;
    private List<String> imageUrls;
}
