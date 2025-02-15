package com.picktory.domain.gift.dto.s3;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PresignedUrlRequest {

    @Min(1)
    @Max(5)
    private int count;  // 최소 1개, 최대 5개

    @NotBlank
    private String fileExtension; // 프론트에서 확장자를 요청하도록 변경
}
