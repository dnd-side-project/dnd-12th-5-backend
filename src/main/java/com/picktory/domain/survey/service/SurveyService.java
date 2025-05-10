package com.picktory.domain.survey.service;

import com.picktory.domain.survey.dto.SurveyRequest;
import com.picktory.domain.survey.dto.SurveyResponse;
import com.picktory.domain.survey.entity.Survey;
import com.picktory.domain.survey.repository.SurveyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;

    /**
     * 설문조사 응답 생성
     */
    @Transactional
    public SurveyResponse submitSurvey(Long userId, SurveyRequest request) {
        Survey survey = surveyRepository.save(Survey.create(userId, request.getSurveySatisfaction()));
        return new SurveyResponse(survey.getId(), survey.getSurveySatisfaction());
    }
}
