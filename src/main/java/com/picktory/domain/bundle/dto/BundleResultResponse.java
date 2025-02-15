package com.picktory.domain.bundle.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BundleResultResponse {
    private Long id;
    private List<BundleResultGiftResponse> gifts;
}
