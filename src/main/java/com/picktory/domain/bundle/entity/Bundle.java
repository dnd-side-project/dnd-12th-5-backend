package com.picktory.domain.bundle.entity;

import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DeliveryCharacterType;
import com.picktory.domain.bundle.enums.DesignType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bundles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Bundle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // 연관관계 없이 userId 저장

    @Column(nullable = false, length = 100)
    private String name; // 보따리 이름

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DesignType designType; // 보따리 디자인 타입

    @Enumerated(EnumType.STRING)
    @Column(nullable = true) // NULL 허용
    private DeliveryCharacterType deliveryCharacterType;

    @Column(length = 255)
    private String link; // 배달용 링크 (없으면 PUBLISHED 불가)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BundleStatus status = BundleStatus.DRAFT; // 기본값 설정

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt; // PUBLISHED 상태가 되었을 때

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false; // 응답 확인 여부 (기본값: false)

    /**
     * 보따리 배달 완료 후, 배달 정보가 설정되었을 때 PUBLISHED 상태로 변경
     */
    public void publish(String link, DeliveryCharacterType characterType) {
        if (link == null || link.isEmpty()) {
            throw new IllegalStateException("배달 링크가 설정되지 않았습니다.");
        }
        this.link = link;
        this.deliveryCharacterType = characterType;
        this.status = BundleStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * 응답 완료 시 보따리 상태를 COMPLETED로 변경
     */
    public void complete() {
        if (this.status != BundleStatus.PUBLISHED) {
            throw new IllegalStateException("PUBLISHED 상태에서만 COMPLETED로 변경 가능합니다.");
        }
        this.status = BundleStatus.COMPLETED;
    }

    /**
     * 사용자가 응답을 확인하면 isRead를 true로 변경
     */
    public void markAsRead() {
        if (this.status == BundleStatus.COMPLETED && this.isRead == Boolean.FALSE) {
            this.isRead = true;
        }
    }
}
