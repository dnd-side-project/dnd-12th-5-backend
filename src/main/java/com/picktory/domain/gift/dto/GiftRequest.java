package com.picktory.domain.gift.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
public class GiftRequest extends AbstractGiftRequest {
    @Builder
    public GiftRequest(String name, String message, String purchaseUrl, List<String> imageUrls) {
        this.setName(name);
        this.setMessage(message);
        this.setPurchaseUrl(purchaseUrl);
        this.setImageUrls(imageUrls);
    }

    @Override
    public Long getId() {
        return null; // GiftRequest는 새 선물 생성이므로 ID가 필요 없음
    }
}
