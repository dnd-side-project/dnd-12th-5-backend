package com.picktory.domain.survey.dto;

import com.picktory.domain.survey.enums.SurveySatisfaction;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SurveyResponse {
    private Long surveyId;
    private SurveySatisfaction satisfaction;
}