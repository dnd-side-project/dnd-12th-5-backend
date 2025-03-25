package com.picktory.domain.bundle.dto;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DesignType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BundleDto {
    protected Long id;
    protected String name;
    protected DesignType designType;
    protected LocalDateTime updatedAt;
    protected BundleStatus status;
    protected Boolean isRead;

    public BundleDto(Bundle bundle) {
        this.id = bundle.getId();
        this.name = bundle.getName();
        this.designType = bundle.getDesignType();
        this.updatedAt = bundle.getUpdatedAt();
        this.status = bundle.getStatus();
        this.isRead = bundle.getIsRead();
    }
}
