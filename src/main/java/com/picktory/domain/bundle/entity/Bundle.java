package com.picktory.domain.bundle.entity;

import com.picktory.common.BaseEntity;
import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DeliveryCharacterType;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bundles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Bundle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name; // 보따리 이름

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DesignType designType; // 보따리 디자인 타입

    @Enumerated(EnumType.STRING)
    @Column(nullable = true) // NULL 허용
    private DeliveryCharacterType deliveryCharacterType;

    @Column(unique = true)
    private String link; // 배달용 링크 (없으면 PUBLISHED 불가)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BundleStatus status = BundleStatus.DRAFT;

    private LocalDateTime publishedAt;

    @Builder.Default
    private Boolean isRead = false;


    /**
     * 배달부 캐릭터 설정
     */
    public void updateDeliveryCharacter(DeliveryCharacterType type, String link) {
        validateDraftStatus();
        validateDeliveryCharacter(type);

        this.deliveryCharacterType = type;
        this.link = link;
        this.status = BundleStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * DRAFT 상태 검증
     */
    private void validateDraftStatus() {
        if (this.status != BundleStatus.DRAFT) {
            throw new BaseException(BaseResponseStatus.INVALID_BUNDLE_STATUS);
        }
    }

    /**
     * 보따리 배달 완료 후, 배달 정보가 설정되었을 때 PUBLISHED 상태로 변경
     */
    public void publish(String link, DeliveryCharacterType characterType) {
        if (link == null || link.isEmpty()) {
            throw new BaseException(BaseResponseStatus.INVALID_CHARACTER_TYPE);
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
            throw new BaseException(BaseResponseStatus.INVALID_BUNDLE_STATUS_FOR_COMPLETE);
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
    private void validateDeliveryCharacter(DeliveryCharacterType type) {
        if (type == null) {
            throw new BaseException(BaseResponseStatus.INVALID_CHARACTER_TYPE);
        }
    }

    /**
     * 보따리의 이름을 업데이트
     */
    public void updateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BaseException(BaseResponseStatus.BUNDLE_NAME_REQUIRED);
        }
        this.name = name;
    }
}
