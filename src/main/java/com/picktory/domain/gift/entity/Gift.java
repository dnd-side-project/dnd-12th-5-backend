package com.picktory.domain.gift.entity;

import com.picktory.domain.gift.dto.AbstractGiftRequest;
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
    private String name; // 선물명

    @Column(columnDefinition = "TEXT")
    private String message; // 선물에 첨부할 메시지

    @Column(columnDefinition = "TEXT")
    private String purchaseUrl; // 구매 링크

    @Enumerated(EnumType.STRING)
    private GiftResponseTag responseTag; // 선물 응답 타입

    @Column(nullable = false)
    @Builder.Default
    private Boolean isResponsed = false; // 응답 여부

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static Gift createGift(Long bundleId, AbstractGiftRequest request) {
        return Gift.builder()
                .bundleId(bundleId)
                .name(request.getName())
                .message(request.getMessage())
                .purchaseUrl(request.getPurchaseUrl())
                .responseTag(null)
                .isResponsed(false)
                .build();
    }

    public static Gift updateGift(Gift existingGift, GiftUpdateRequest request) {
        existingGift.name = request.getName();
        existingGift.message = request.getMessage();
        existingGift.purchaseUrl = request.getPurchaseUrl();
        return existingGift;
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

    // 응답 상태 및 태그 변경 메서드 추가
    public void updateResponse(GiftResponseTag responseTag) {
        this.responseTag = responseTag;
        this.isResponsed = true;
    }
}
