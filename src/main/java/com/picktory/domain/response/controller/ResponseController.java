package com.picktory.domain.response.controller;

import com.picktory.common.BaseResponse;
import com.picktory.domain.response.dto.ResponseBundleDto;
import com.picktory.domain.response.dto.SaveGiftResponsesRequest;
import com.picktory.domain.response.dto.SaveGiftResponsesResponse;
import com.picktory.domain.response.service.ResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ResponseController {
    private final ResponseService responseService;

    @GetMapping("/responses/bundles/{link}")
    public ResponseEntity<BaseResponse<ResponseBundleDto>> getBundleByLink(@PathVariable String link) {
        ResponseBundleDto response = responseService.getBundleByLink(link);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }

    @PostMapping("/responses/bundles/{link}/answers")
    public ResponseEntity<BaseResponse<SaveGiftResponsesResponse>> saveGiftResponses(
            @PathVariable String link,
            @RequestBody SaveGiftResponsesRequest request) {
        SaveGiftResponsesResponse response = responseService.saveGiftResponses(link, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponse<>(response));
    }
}