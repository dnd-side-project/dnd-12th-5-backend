package com.picktory.domain.gift.entity;

import com.picktory.domain.gift.dto.GiftRequest;
import com.picktory.domain.gift.dto.GiftUpdateRequest;
import com.picktory.domain.gift.enums.GiftResponseTag;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gifts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Gift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bundleId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String purchaseUrl;

    @Enumerated(EnumType.STRING)
    private GiftResponseTag responseTag;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isResponsed = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static Gift createGift(Long bundleId, GiftRequest request) {
        return Gift.builder()
                .bundleId(bundleId)
                .name(request.getName())
                .message(request.getMessage())
                .purchaseUrl(request.getPurchaseUrl())
                .responseTag(null)
                .isResponsed(false)
                .build();
    }

    // 응답 상태 변경 메서드
    public void setResponded(boolean responded) {
        this.isResponsed = responded;
    }

    // 기존 선물 정보 업데이트
    public void updateGift(GiftUpdateRequest request) {
        this.name = request.getName();
        this.message = request.getMessage();
        this.purchaseUrl = request.getPurchaseUrl();
    }
}