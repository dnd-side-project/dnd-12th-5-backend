package com.picktory.domain.gift.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.config.auth.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;


    private final S3Client s3Client;
    private final AuthenticationService authenticationService;

    // 허용된 이미지 타입 목록
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif"
    );

    /**
     * 여러 개의 이미지 업로드 처리
     */
    public List<String> uploadImages(List<MultipartFile> files) {
        Long userId = authenticationService.getAuthenticatedUser().getId();

        if (files.isEmpty() || files.size() > 5) {
            throw new BaseException(BaseResponseStatus.GIFT_IMAGE_COUNT);
        }

        return files.stream()
                .map(file -> uploadFileToS3(file, userId))
                .collect(Collectors.toList());
    }

    /**
     * 개별 이미지 파일을 S3에 업로드하고, DB에 저장할 URL 반환
     */
    private String uploadFileToS3(MultipartFile file, Long userId) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BaseException(BaseResponseStatus.INVALID_GIFT_IMAGE_TYPE);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (extension == null) {
            throw new BaseException(BaseResponseStatus.INVALID_GIFT_IMAGE_TYPE);
        }

        // S3에 저장될 파일 경로 생성
        String filePath = getFilePath(userId, extension);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // DB에 저장할 URL 반환
            return getFileUrl(filePath);
        } catch (IOException e) {
            log.error("S3 업로드 실패: {}", e.getMessage(), e);
            throw new BaseException(BaseResponseStatus.SERVER_ERROR);
        }
    }

    /**
     * S3에 저장될 파일 경로 생성
     */
    private String getFilePath(Long userId, String extension) {
        return String.format("gifts/users/%d/%s.%s", userId, UUID.randomUUID(), extension);
    }

    /**
     * DB에 저장될 S3 접근 가능한 URL 생성
     */
    private String getFileUrl(String filePath) {
//        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, filePath);
        return String.format("https://%s/%s", cloudFrontDomain, filePath);
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
