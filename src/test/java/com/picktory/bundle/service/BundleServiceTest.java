package com.picktory.bundle.service;

import com.picktory.config.auth.AuthenticationService;
import com.picktory.domain.bundle.dto.BundleRequest;
import com.picktory.domain.bundle.dto.BundleResponse;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.bundle.service.BundleService;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.gift.dto.GiftRequest;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import com.picktory.domain.user.entity.User;
import com.picktory.support.config.jwt.TestJwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BundleServiceTest {

    @Mock
    private BundleRepository bundleRepository;

    @Mock
    private GiftRepository giftRepository;

    @Mock
    private GiftImageRepository giftImageRepository;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private BundleService bundleService;

    // ✅ 공통 JWT 사용
    private static final String TEST_JWT_TOKEN = TestJwtTokenProvider.generateTestToken("1");

    @Test
    void 보따리_최초생성_선물포함_테스트() {
        // ✅ Given (Mock 유저 생성)
        User mockUser = User.builder()
                .kakaoId("testKakaoId")
                .nickname("TestUser")
                .build();

        when(authenticationService.getAuthenticatedUser()).thenReturn(mockUser);

        // ✅ Given (선물 목록 생성)
        GiftRequest giftRequest1 = new GiftRequest();
        giftRequest1.setMessage("첫 번째 선물 메시지");
        giftRequest1.setImageUrls(List.of("http://image1.com"));

        GiftRequest giftRequest2 = new GiftRequest();
        giftRequest2.setMessage("두 번째 선물 메시지");
        giftRequest2.setImageUrls(List.of("http://image2.com"));

        List<GiftRequest> giftRequests = Arrays.asList(giftRequest1, giftRequest2);

        // ✅ Given (보따리 생성 요청)
        BundleRequest request = new BundleRequest();
        request.setName("Test Bundle");
        request.setDesignType(DesignType.RED);
        request.setGifts(giftRequests);

        // ✅ Given (Mock Bundle 저장)
        Bundle mockBundle = Bundle.builder()
                .id(1L)
                .userId(1L)
                .name(request.getName())
                .designType(request.getDesignType())
                .status(BundleStatus.DRAFT)
                .isRead(false)
                .build();

        when(bundleRepository.save(any(Bundle.class))).thenReturn(mockBundle);

        // ✅ Given (Mock 선물 저장)
        Gift mockGift1 = Gift.builder()
                .id(100L)
                .bundleId(mockBundle.getId())
                .message("첫 번째 선물 메시지")
                .build();

        Gift mockGift2 = Gift.builder()
                .id(101L)
                .bundleId(mockBundle.getId())
                .message("두 번째 선물 메시지")
                .build();

        List<Gift> mockGifts = List.of(mockGift1, mockGift2);
        when(giftRepository.saveAll(any())).thenReturn(mockGifts);

        // ✅ Given (Mock 선물 이미지 저장)
        GiftImage mockImage1 = GiftImage.builder()
                .id(1000L)
                .giftId(100L)
                .imageUrl("http://image1.com")
                .build();

        GiftImage mockImage2 = GiftImage.builder()
                .id(1001L)
                .giftId(101L)
                .imageUrl("http://image2.com")
                .build();

        List<GiftImage> mockGiftImages = List.of(mockImage1, mockImage2);
        when(giftImageRepository.saveAll(any())).thenReturn(mockGiftImages);

        // ✅ When (보따리 생성)
        BundleResponse response = bundleService.createBundle(request);

        // ✅ Then (검증)
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Bundle");
        assertThat(response.getDesignType()).isEqualTo(request.getDesignType());
        assertThat(response.getStatus()).isEqualTo(BundleStatus.DRAFT);
        assertThat(response.getGifts()).hasSize(2);  // ✅ 저장된 선물 개수 검증
        assertThat(response.getGifts().get(0).getMessage()).isEqualTo("첫 번째 선물 메시지");
        assertThat(response.getGifts().get(1).getMessage()).isEqualTo("두 번째 선물 메시지");

        // ✅ 로그 출력
        System.out.println("✅ 생성된 보따리: " + response);
    }
}
