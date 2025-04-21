package com.picktory.domain.response.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SaveGiftResponsesRequest {
    private String bundleId;
    private List<GiftResponse> gifts;

    @Getter
    @Setter
    public static class GiftResponse {
        private Long giftId;
        private String responseTag;
    }
}