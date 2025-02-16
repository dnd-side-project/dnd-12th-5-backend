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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_id", nullable = false)
    private Gift gift;


    @Column(nullable = false, columnDefinition = "TEXT")
    private String imageUrl; // S3에 저장된 이미지 URL

    @Column(nullable = false)
    private Boolean isPrimary; // 대표 썸네일 여부

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime uploadedAt;

    public static GiftImage createGiftImage(Gift gift, String imageUrl, boolean isPrimary) {
        return GiftImage.builder()
                .gift(gift)
                .imageUrl(imageUrl)
                .isPrimary(isPrimary)
                .uploadedAt(LocalDateTime.now())
                .build();
    }
    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    public Long getGiftId() {
        return this.gift.getId();
    }
    public static class GiftImageBuilder {
        public GiftImageBuilder giftId(Long giftId) {
            this.gift = Gift.builder().id(giftId).build();
            return this;
        }
    }
}
