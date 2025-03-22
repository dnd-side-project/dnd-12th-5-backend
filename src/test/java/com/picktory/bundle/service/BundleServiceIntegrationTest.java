package com.picktory.bundle.service;

import com.picktory.config.auth.AuthenticationService;
import com.picktory.domain.bundle.dto.BundleRequest;
import com.picktory.domain.bundle.dto.BundleResponse;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.bundle.service.BundleService;
import com.picktory.domain.gift.dto.GiftRequest;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/*
✅ MySQL과 연동한 테스트
*/

@SpringBootTest
@Transactional
@Rollback(false) // ⚠️ 테스트 후 데이터 정리 필요
class BundleServiceIntegrationTest {

    @Autowired
    private BundleService bundleService;

    @Autowired
    private BundleRepository bundleRepository;

    @Autowired
    private GiftRepository giftRepository;

    @Autowired
    private GiftImageRepository giftImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationService authenticationService; // ✅ Mock을 제거하고 실제 동작하게 함

    private User testUser;

    @BeforeEach
    void setUp() {
        // ✅ MySQL에서 testuser 가져오기 (없으면 자동 생성)
        testUser = userRepository.findByKakaoId(12345678L)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .kakaoId(12345678L)
                            .nickname("testusernick")
                            .build();
                    return userRepository.save(newUser);
                });

        // ✅ 실제 인증 로직이 동작하도록 설정
        System.out.println("🔥 테스트 유저: " + testUser.getId() + " / " + testUser.getKakaoId());
    }

//    @AfterEach
//    void tearDown() {
//        bundleRepository.deleteAll(); // ✅ 테스트 후 데이터 삭제
//    }

    @Test
    @WithMockUser(username = "12345678") // ✅ 테스트에서 인증된 사용자로 가정
    @DisplayName("✅ MySQL 연동 - 보따리 최초 생성 성공")
    void 보따리_최초_생성_테스트() {
        // Given: 보따리 생성 요청 데이터
        BundleRequest request = new BundleRequest();
        request.setName("내 생일 보따리");
        request.setDesignType(DesignType.RED);
        request.setGifts(List.of(
                new GiftRequest("향수", "좋은 향기로 기억되길!", "https://example.com/perfume",
                        List.of("https://s3.example.com/image1.jpg", "https://s3.example.com/image2.jpg")),
                new GiftRequest("초콜릿", "달콤한 하루 보내!", "https://example.com/chocolate",
                        List.of("https://s3.example.com/chocolate1.jpg", "https://s3.example.com/chocolate2.jpg")),
                new GiftRequest("사탕", "안녕!", "https://example.com/candy",
                        List.of("https://s3.example.com/candyfirst.jpg", "https://s3.example.com/candy.jpg", "https://s3.example.com/candy3.jpg"))

        ));

        // When: API 호출
        BundleResponse response = bundleService.createBundle(request);

        // Then: 응답 데이터 검증
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("내 생일 보따리");
        assertThat(response.getDesignType()).isEqualTo(DesignType.RED);
        assertThat(response.getStatus()).isEqualTo(BundleStatus.DRAFT);
        assertThat(response.getGifts()).hasSize(3);

        // DB 저장 데이터 검증
        Bundle savedBundle = bundleRepository.findById(response.getId()).orElseThrow();
        assertThat(savedBundle.getName()).isEqualTo("내 생일 보따리");
        assertThat(savedBundle.getStatus()).isEqualTo(BundleStatus.DRAFT);

        // 선물 및 이미지 데이터 검증
        List<Gift> savedGifts = giftRepository.findAllByBundleId(savedBundle.getId());
        assertThat(savedGifts).hasSize(3);

        List<GiftImage> savedImages = giftImageRepository.findAll();
        assertThat(savedImages).hasSize(7); // 총 7개의 이미지가 저장되어야 함 (각 선물당 2,2,3개씩)

        // 대표 이미지 검증
        for (GiftImage image : savedImages) {
            if (image.getImageUrl().equals("https://s3.example.com/image1.jpg") ||
                    image.getImageUrl().equals("https://s3.example.com/chocolate1.jpg") ||
                        image.getImageUrl().equals("https://s3.example.com/candyfirst.jpg")) {
                assertThat(image.getIsPrimary()).isTrue(); // 첫 번째 이미지는 대표 이미지여야 함
            } else {
                assertThat(image.getIsPrimary()).isFalse(); // 나머지는 대표 이미지가 아니어야 함
            }
        }

        // ✅ 콘솔 로그 확인
        System.out.println("✅ 보따리 ID: " + response.getId());
        System.out.println("✅ 선물 개수: " + savedGifts.size());
        savedImages.forEach(img ->
                System.out.println("✅ 저장된 이미지: " + img.getImageUrl() + " (대표: " + img.getIsPrimary() + ")")
        );
    }
}
