package com.picktory.domain.gift.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gift_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GiftImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long giftId;  // 선물 ID 저장

    @Column(nullable = false, columnDefinition = "TEXT")
    private String s3Url; // S3에 저장된 이미지 URL

    @Column(nullable = false)
    private Boolean isPrimary; // 대표 썸네일 여부

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt; // 이미지 업로드 시각

    @PrePersist
    protected void onUpload() {
        this.uploadedAt = LocalDateTime.now();
    }

    public static GiftImage createGiftImage(Long giftId, String s3Url, boolean isPrimary) {
        return GiftImage.builder()
                .giftId(giftId)
                .s3Url(s3Url)
                .isPrimary(isPrimary)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
