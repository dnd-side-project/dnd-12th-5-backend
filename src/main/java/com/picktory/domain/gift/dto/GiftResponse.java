package com.picktory.domain.gift.dto;

import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.enums.GiftResponseTag;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class GiftResponse {

    private Long id;
    private String name;
    private String message;
    private String purchaseUrl;
    private GiftResponseTag responseTag;
    private Boolean isResponsed;
    private List<String> imageUrls;

    public static GiftResponse fromEntity(Gift gift, List<GiftImage> images) {
        return GiftResponse.builder()
                .id(gift.getId())
                .name(gift.getName())
                .message(gift.getMessage())
                .purchaseUrl(gift.getPurchaseUrl())
                .responseTag(gift.getResponseTag())
                .isResponsed(gift.getIsResponsed())
                .imageUrls(images == null ? Collections.emptyList() :
                        images.stream().map(GiftImage::getImageUrl).collect(Collectors.toList()))
                .build();
    }

}
