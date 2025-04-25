package com.picktory.domain.bundle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BundleNameUpdateRequest {
    @NotBlank(message = "보따리 이름은 필수입니다.")
    @Size(max = 100, message = "보따리 이름은 최대 100자까지 입력할 수 있습니다.")
    private String name;
}