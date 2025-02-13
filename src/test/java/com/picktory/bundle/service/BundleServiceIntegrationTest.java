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
MySQL과 연동한 테스트
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
    private UserRepository userRepository;

    @Autowired
    private AuthenticationService authenticationService; // ✅ Mock을 제거하고 실제 동작하게 함

    private User testUser;

    @BeforeEach
    void setUp() {
        // ✅ MySQL에서 testuser 가져오기 (없으면 자동 생성)
        testUser = userRepository.findByKakaoId("testuserkakao")
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .kakaoId("testuserkakao")
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
    @WithMockUser(username = "testuserkakao") // ✅ 테스트에서 인증된 사용자로 가정
    @DisplayName("✅ MySQL 연동 - 보따리 최초 생성 성공")
    void 보따리_최초생성_테스트_MySQL() {
        // Given - 보따리 생성 요청
        GiftRequest giftRequest1 = new GiftRequest();
        giftRequest1.setName("첫선물");
        giftRequest1.setMessage("첫 번째 선물내용");
        giftRequest1.setImageUrls(List.of("http://image1.com"));

        GiftRequest giftRequest2 = new GiftRequest();
        giftRequest2.setName("둘째선물");
        giftRequest2.setMessage("두 번째 선물내용");
        giftRequest2.setImageUrls(List.of("http://image2.com"));

        BundleRequest request = new BundleRequest();
        request.setName("Test Bundle");
        request.setDesignType(DesignType.RED);
        request.setGifts(List.of(giftRequest1, giftRequest2));

        // ✅ When - 보따리 생성
        BundleResponse response = bundleService.createBundle(request);

        // ✅ Then - 저장된 데이터 검증
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getDesignType()).isEqualTo(request.getDesignType());
        assertThat(response.getStatus()).isEqualTo(BundleStatus.DRAFT);
        assertThat(response.getGifts()).hasSize(2);

        // ✅ MySQL에 실제 저장된 보따리 확인
        Bundle savedBundle = bundleRepository.findById(response.getId()).orElseThrow();
        assertThat(savedBundle.getName()).isEqualTo("Test Bundle");

        // ✅ MySQL에 실제 저장된 선물 확인
        List<Gift> savedGifts = giftRepository.findByBundleId(savedBundle.getId());
        assertThat(savedGifts).hasSize(2);
    }

}
