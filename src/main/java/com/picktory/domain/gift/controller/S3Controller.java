package com.picktory.domain.gift.controller;

import com.picktory.common.BaseResponse;
import com.picktory.domain.gift.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gifts/images")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    /**
     * S3 이미지 MultipartFile식 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<BaseResponse<Map<String, List<String>>>> uploadImages(@RequestParam List<MultipartFile> files) {
        List<String> s3Urls = s3Service.uploadImages(files);
        return ResponseEntity.ok(new BaseResponse<>(Map.of("uploadedUrls", s3Urls)));
    }
}
