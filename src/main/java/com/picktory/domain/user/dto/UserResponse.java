package com.picktory.domain.user.dto;

import com.picktory.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

/**
 * 사용자 정보 응답 DTO
 * 사용자 정보를 클라이언트에게 전달하기 위한 객체입니다.
 */
@Getter
@Builder
public class UserResponse {
    /**
     * 사용자 ID
     */
    private Long id;

    /**
     * 사용자 닉네임
     */
    private String nickname;

    /**
     * 생성 일시
     */
    private String createdAt;

    /**
     * 삭제 여부
     */
    private boolean isDeleted;

    /**
     * 삭제 일시
     */
    private String deletedAt;

    /**
     * User 엔티티를 UserResponse DTO로 변환합니다.
     *
     * @param user 사용자 엔티티
     * @return 사용자 응답 DTO
     */
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