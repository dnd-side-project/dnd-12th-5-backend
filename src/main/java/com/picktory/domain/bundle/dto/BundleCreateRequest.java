package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.enums.DesignType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BundleCreateRequest {

    @NotNull
    private String name;

    @NotNull
    private DesignType designType;

    private String link;
}
