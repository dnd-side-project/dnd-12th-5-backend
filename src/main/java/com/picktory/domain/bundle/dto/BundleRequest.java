package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DeliveryCharacterType;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.gift.dto.GiftRequest;
import com.picktory.domain.user.entity.User;
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

    @NotNull(message = "보따리 이름은 필수입니다.")
    private String name;

    @NotNull(message = "디자인 타입은 필수입니다.")
    private DesignType designType;

    @NotNull
    @Size(min = 1, max = 6, message = "보따리에는 최소 1개, 최대 6개의 선물을 담아야 합니다.")
    private List<GiftRequest> gifts;

    public Bundle toEntity(User user) {
        return Bundle.builder()
                .user(user)
                .name(this.name)
                .designType(this.designType)
                .status(BundleStatus.DRAFT)
                .isRead(false)
                .build();
    }
}
