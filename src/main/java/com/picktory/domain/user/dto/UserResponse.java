package com.picktory.domain.user.dto;

import com.picktory.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String nickname;
    private String createdAt;
    private boolean isDeleted;
    private String deletedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt().toString())
                .isDeleted(user.isDeleted())
                .deletedAt(user.getDeletedAt() != null ? user.getDeletedAt().toString() : null)
                .build();
    }
}