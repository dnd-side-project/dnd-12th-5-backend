package com.picktory.domain.auth.refresh.entity;

import com.picktory.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, name = "expiry_date")
    private LocalDateTime expiryDate;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public void updateToken(String token, LocalDateTime expiryDate) {
        this.token = token;
        this.expiryDate = expiryDate;
    }

    public static RefreshToken create(Long userId, String token, LocalDateTime expiryDate) {
        return RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiryDate(expiryDate)
                .build();
    }
}