package com.picktory.domain.response.entity;

import com.picktory.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Response extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gift_id", nullable = false)
    private Long giftId;

    @Column(name = "bundle_id", nullable = false)
    private Long bundleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_tag", nullable = false)
    private ResponseTag responseTag;

    @Column(columnDefinition = "TEXT")
    private String message;
}
