package com.picktory.config.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3Presigner 설정: EC2환경에서는 IAM Role을 기본으로 사용하며, 로컬에선 Access Key 인증
 */
@Slf4j
@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key:}")  // 값이 없으면 빈 문자열 반환
    private String accessKey;

    @Value("${aws.s3.secret-key:}")  // 값이 없으면 빈 문자열 반환
    private String secretKey;

    /**
     * S3Presigner Bean 생성 (IAM Role → Access Key 순으로 인증)
     */
    @Bean
    public S3Presigner s3Presigner() {
        AwsCredentialsProvider credentialsProvider = getCredentialsProvider();

        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * IAM Role이 존재하면 사용, 없으면 Access Key 사용
     */
    private AwsCredentialsProvider getCredentialsProvider() {
        try {
            // 우선 IAM Role을 사용
            AwsCredentialsProvider defaultProvider = DefaultCredentialsProvider.create();
            defaultProvider.resolveCredentials();  // IAM Role이 존재하는지 체크
            log.info("IAM Role을 사용하여 AWS 인증을 수행합니다.");
            return defaultProvider;
        } catch (Exception e) {
            // IAM Role이 없을 경우 Access Key 사용
            if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
                log.warn("IAM Role을 찾을 수 없습니다. Access Key & Secret Key 인증을 사용합니다.");
                return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
            } else {
                throw new RuntimeException("AWS 인증 정보를 찾을 수 없습니다. IAM Role 또는 Access Key를 설정하세요.");
            }
        }
    }
}
