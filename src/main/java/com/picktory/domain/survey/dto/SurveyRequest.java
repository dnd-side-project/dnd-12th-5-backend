package com.picktory.domain.survey.dto;

import com.picktory.domain.survey.enums.SurveySatisfaction;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SurveyRequest {

    @NotNull(message = "설문 만족도가 선택되지 않았습니다.")
    private SurveySatisfaction surveySatisfaction;

}
