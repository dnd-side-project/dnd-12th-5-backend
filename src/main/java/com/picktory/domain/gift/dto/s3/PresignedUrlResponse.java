package com.picktory.domain.gift.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignedUrlResponse {
    private String presignedUrl;
    private int expiresIn;
}
