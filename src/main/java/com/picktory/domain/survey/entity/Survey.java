package com.picktory.domain.survey.entity;

import com.picktory.common.BaseEntity;
import com.picktory.domain.survey.enums.SurveySatisfaction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "surveys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Survey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SurveySatisfaction surveySatisfaction;


    private Survey(Long userId, SurveySatisfaction satisfaction) {
        this.userId = userId;
        this.surveySatisfaction = satisfaction;
    }

    public static Survey create(Long userId, SurveySatisfaction satisfaction) {
        return new Survey(userId, satisfaction);
    }
}
