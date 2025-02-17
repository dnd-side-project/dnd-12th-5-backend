package com.picktory.gift.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class GiftServiceTest {

    @Autowired
    private S3Client s3Client;

    @Test
    public void testS3Connection() {
        assertNotNull(s3Client, "S3Client가 정상적으로 주입되지 않았습니다.");
        System.out.println("S3 연결 성공!");
    }
}
