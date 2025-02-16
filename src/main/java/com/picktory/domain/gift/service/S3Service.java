package com.picktory.domain.gift.service;

import com.picktory.config.auth.AuthenticationService;
import com.picktory.domain.gift.dto.s3.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private final S3Presigner s3Presigner;
    private final AuthenticationService authenticationService;

    // 허용된 확장자 목록 (jpg, jpeg, png, webp)
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    /**
     * Presigned URL 리스트 생성 요청
     */
    public List<PresignedUrlResponse> generatePresignedUrls(int count, String fileExtension) {
        // 현재 로그인한 사용자 가져오기
        Long userId = authenticationService.getAuthenticatedUser().getId();

        // 개수 검증
        if (count < 1 || count > 5) {
            throw new IllegalArgumentException("이미지 개수는 최소 1개, 최대 5개여야 합니다.");
        }

        // 확장자 검증
        final String normalizedExtension = fileExtension.toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(normalizedExtension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 확장자입니다: " + normalizedExtension);
        }

        log.info("S3 Presigned URL {}개 생성 시작 for userId: {}, 확장자: {}", count, userId, normalizedExtension);

        return IntStream.range(0, count)
                .mapToObj(i -> createPresignedUrl(userId, normalizedExtension))
                .toList();
    }

    /**
     * Presigned URL 생성 로직
     */
    private PresignedUrlResponse createPresignedUrl(Long userId, String fileExtension) {
        // 사용자 ID 기반으로 S3 파일 경로 설정
        String fileName = String.format("gifts/users/%d/%s.%s", userId, UUID.randomUUID(), fileExtension);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        // Presigned URL 생성
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(24)) // 24시간 유효
                        .putObjectRequest(objectRequest)
                        .build()
        );

        log.info("Presigned URL 생성 완료 - 파일명: {}", fileName);

        return new PresignedUrlResponse(presignedRequest.url().toString(), 86400);
    }
}
