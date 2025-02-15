package com.picktory.domain.response.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SaveGiftResponsesResponse {
    private int answeredCount;
    private int totalCount;

    public static SaveGiftResponsesResponse of(int answeredCount, int totalCount) {
        return SaveGiftResponsesResponse.builder()
                .answeredCount(answeredCount)
                .totalCount(totalCount)
                .build();
    }
}