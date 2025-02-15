package com.picktory.domain.response.controller;

import com.picktory.common.BaseResponse;
import com.picktory.domain.response.dto.ResponseBundleDto;
import com.picktory.domain.response.service.ResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/responses")
public class ResponseController {
    private final ResponseService responseService;

    @GetMapping("/bundles/{link}")
    public ResponseEntity<BaseResponse<ResponseBundleDto>> getBundleByLink(@PathVariable String link) {
        ResponseBundleDto response = responseService.getBundleByLink(link);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }
}