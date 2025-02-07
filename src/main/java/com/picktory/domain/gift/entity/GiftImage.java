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
    private String imageUrl; // S3에 저장된 이미지 URL

    @Column(nullable = false)
    private Boolean isPrimary; // 대표 썸네일 여부

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime uploadedAt;

    public static GiftImage createGiftImage(Long giftId, String imageUrl, boolean isPrimary) {
        return GiftImage.builder()
                .giftId(giftId)
                .imageUrl(imageUrl)
                .isPrimary(isPrimary)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}
