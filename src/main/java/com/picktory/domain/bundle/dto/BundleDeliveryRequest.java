package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.enums.DeliveryCharacterType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BundleDeliveryRequest {
    @NotNull(message = "배달부 캐릭터를 선택해주세요.")
    private DeliveryCharacterType deliveryCharacterType;
}