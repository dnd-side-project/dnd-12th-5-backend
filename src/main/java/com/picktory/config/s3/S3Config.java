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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
public class S3Config {

    @Value("${aws.s3.region:ap-northeast-2}")
    private String region;

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;

    /**
     * S3Client Bean 생성 (IAM Role → Access Key 순으로 인증)
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    /**
     * IAM Role이 존재하면 사용, 없으면 Access Key 사용
     */
    private AwsCredentialsProvider getCredentialsProvider() {
        try {
            // IAM Role이 있는지 확인하고 기본 제공 인증 사용
            AwsCredentialsProvider defaultProvider = DefaultCredentialsProvider.create();
            defaultProvider.resolveCredentials();
            log.info("IAM Role이 있습니다. IAM Role을 사용하여 AWS 인증을 수행합니다.");
            return defaultProvider;
        } catch (Exception e) {
            // IAM Role이 없으면 Access Key 사용
            if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
                log.warn("AWS IAM Role을 찾을 수 없습니다. Access Key & Secret Key 인증을 사용합니다.");
                return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
            } else {
                throw new RuntimeException("AWS 인증 정보를 찾을 수 없습니다. IAM Role 또는 Access Key를 설정하세요.");
            }
        }
    }
}
