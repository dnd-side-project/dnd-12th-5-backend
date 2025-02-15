package com.picktory.domain.gift.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
public class GiftUpdateRequest extends AbstractGiftRequest {
    @Builder
    public GiftUpdateRequest(Long id, String name, String message, String purchaseUrl, List<String> imageUrls) {
        this.setId(id);
        this.setName(name);
        this.setMessage(message);
        this.setPurchaseUrl(purchaseUrl);
        this.setImageUrls(imageUrls);
    }
}
