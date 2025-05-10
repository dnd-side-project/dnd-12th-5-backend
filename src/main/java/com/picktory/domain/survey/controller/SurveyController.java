package com.picktory.domain.survey.controller;

import com.picktory.common.BaseResponse;
import com.picktory.config.auth.AuthenticationService;
import com.picktory.domain.survey.dto.SurveyRequest;
import com.picktory.domain.survey.dto.SurveyResponse;
import com.picktory.domain.survey.service.SurveyService;
import com.picktory.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {
    private final SurveyService surveyService;
    private final AuthenticationService authenticationService;

    @PostMapping
    public ResponseEntity<BaseResponse<SurveyResponse>> submitSurvey(@Valid @RequestBody SurveyRequest request) {
        User currentUser = authenticationService.getAuthenticatedUser();
        SurveyResponse response = surveyService.submitSurvey(currentUser.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new BaseResponse<>(true, 201, "설문 응답이 성공적으로 저장되었습니다.", response));

    }
}
