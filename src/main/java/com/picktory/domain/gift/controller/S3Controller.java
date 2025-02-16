package com.picktory.domain.gift.controller;

import com.picktory.domain.gift.dto.s3.PresignedUrlRequest;
import com.picktory.domain.gift.dto.s3.PresignedUrlResponse;
import com.picktory.domain.gift.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/gifts/images")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    /**
     * Presigned URL 생성 API
     */
    @PostMapping("/presigned-urls")
    public ResponseEntity<Map<String, List<PresignedUrlResponse>>> generatePresignedUrls(
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        log.info("Presigned URL 요청 - count: {}, 확장자: {}", request.getCount(), request.getFileExtension());

        // 서비스에서 인증된 사용자 확인 및 Presigned URL 생성
        List<PresignedUrlResponse> urls = s3Service.generatePresignedUrls(request.getCount(), request.getFileExtension());

        log.info("Presigned URL {}개 생성 완료", urls.size());

        return ResponseEntity.ok(Map.of("presignedUrls", urls));
    }
}
