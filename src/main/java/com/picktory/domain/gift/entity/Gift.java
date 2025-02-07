package com.picktory.domain.gift.entity;

import com.picktory.domain.gift.dto.GiftRequest;
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

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

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
}
