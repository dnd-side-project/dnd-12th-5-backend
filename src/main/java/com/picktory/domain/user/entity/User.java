package com.picktory.domain.user.entity;

import com.picktory.common.BaseEntity;
import com.picktory.common.exception.BaseException;
import com.picktory.common.BaseResponseStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long kakaoId;

    private String nickname;

    @Column(nullable = false)
    private boolean isDeleted;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private boolean isSurveyExcluded = false;

    @Builder
    public User(Long id, Long kakaoId, String nickname) {
        this.id = id;
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.isDeleted = false;
    }

    public void delete() {
        if (this.isDeleted) {
            throw new BaseException(BaseResponseStatus.ALREADY_DELETED_USER);
        }
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void reactivate() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    public void markSurveyExcluded() {
        this.isSurveyExcluded = true;
    }
}