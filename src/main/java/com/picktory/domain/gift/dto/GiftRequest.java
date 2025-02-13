package com.picktory.domain.gift.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiftRequest implements GiftImageRequest {
    @NotNull
    private String name;

    private String message;
    private String purchaseUrl;
    private List<String> imageUrls;

    @Override
    public Long getId() {
        return null; // GiftRequest는 최초 생성이므로 ID가 없음
    }
}
