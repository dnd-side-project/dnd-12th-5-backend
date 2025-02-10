package com.picktory.bundle.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.config.auth.AuthenticationService;
import com.picktory.domain.bundle.dto.BundleRequest;
import com.picktory.domain.bundle.dto.BundleResponse;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.bundle.service.BundleService;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DeliveryCharacterType;
import com.picktory.domain.gift.dto.GiftRequest;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import com.picktory.domain.user.entity.User;
import com.picktory.support.config.jwt.TestJwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .kakaoId("testKakaoId")
                .nickname("TestUser")
                .build();

        // ✅ ID를 명확히 설정- getId()가 null이 되지 않도록
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        when(authenticationService.getAuthenticatedUser()).thenReturn(mockUser);
    }


    @Test
    @DisplayName("✅ 보따리 최초 생성 성공")
    void 보따리_최초생성_테스트() {
        // Given (선물 목록 생성)
        GiftRequest giftRequest1 = new GiftRequest();
        giftRequest1.setMessage("첫 번째 선물 메시지");
        giftRequest1.setImageUrls(List.of("http://image1.com"));

        GiftRequest giftRequest2 = new GiftRequest();
        giftRequest2.setMessage("두 번째 선물 메시지");
        giftRequest2.setImageUrls(List.of("http://image2.com"));

        List<GiftRequest> giftRequests = Arrays.asList(giftRequest1, giftRequest2);

        // Given (보따리 생성 요청)
        BundleRequest request = new BundleRequest();
        request.setName("Test Bundle");
        request.setDesignType(DesignType.RED);
        request.setGifts(giftRequests);

        // Given (Mock Bundle 저장)
        Bundle mockBundle = Bundle.builder()
                .id(1L)
                .userId(1L)
                .name(request.getName())
                .designType(request.getDesignType())
                .status(BundleStatus.DRAFT)
                .isRead(false)
                .build();

        when(bundleRepository.save(any(Bundle.class))).thenReturn(mockBundle);

        // Given (Mock 선물 저장)
        Gift mockGift1 = Gift.builder().id(100L).bundleId(mockBundle.getId()).message("첫 번째 선물 메시지").build();
        Gift mockGift2 = Gift.builder().id(101L).bundleId(mockBundle.getId()).message("두 번째 선물 메시지").build();
        when(giftRepository.saveAll(any())).thenReturn(List.of(mockGift1, mockGift2));

        // Given (Mock 선물 이미지 저장)
        GiftImage mockImage1 = GiftImage.builder().id(1000L).giftId(100L).imageUrl("http://image1.com").build();
        GiftImage mockImage2 = GiftImage.builder().id(1001L).giftId(101L).imageUrl("http://image2.com").build();
        when(giftImageRepository.saveAll(any())).thenReturn(List.of(mockImage1, mockImage2));

        // When (보따리 생성)
        BundleResponse response = bundleService.createBundle(request);

        // Then (검증)
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Bundle");
        assertThat(response.getDesignType()).isEqualTo(request.getDesignType());
        assertThat(response.getStatus()).isEqualTo(BundleStatus.DRAFT);
        assertThat(response.getGifts()).hasSize(2);
    }

    @Test
    @DisplayName("❌ 하루 최대 보따리 개수 초과")
    void 보따리_생성_실패_하루최대개수초과() {
        // Given
        when(bundleRepository.countByUserIdAndCreatedAtAfter(any(Long.class), any(LocalDateTime.class)))
                .thenReturn(10L); // 이미 10개 생성됨

        BundleRequest request = new BundleRequest();
        request.setName("Test Bundle");
        request.setDesignType(DesignType.RED);
        request.setGifts(Arrays.asList(new GiftRequest(), new GiftRequest()));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> bundleService.createBundle(request));
        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_DAILY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("❌ 보따리 이름 없음")
    void 보따리_생성_실패_이름없음() {
        // Given
        BundleRequest request = new BundleRequest();
        request.setName(""); // 빈 문자열
        request.setDesignType(DesignType.RED);
        request.setGifts(Arrays.asList(new GiftRequest(), new GiftRequest()));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> bundleService.createBundle(request));
        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_NAME_REQUIRED);
    }

    @Test
    @DisplayName("❌ 보따리 디자인 타입 없음")
    void 보따리_생성_실패_디자인타입없음() {
        // Given
        BundleRequest request = new BundleRequest();
        request.setName("Test Bundle");
        request.setDesignType(null); // 디자인 타입 없음
        request.setGifts(Arrays.asList(new GiftRequest(), new GiftRequest()));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> bundleService.createBundle(request));
        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_DESIGN_REQUIRED);
    }

    @Test
    @DisplayName("❌ 보따리에 선물이 2개 미만")
    void 보따리_생성_실패_선물_2개미만() {
        // Given
        BundleRequest request = new BundleRequest();
        request.setName("Test Bundle");
        request.setDesignType(DesignType.RED);
        request.setGifts(Collections.singletonList(new GiftRequest())); // 1개만 추가

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> bundleService.createBundle(request));
        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_MINIMUM_GIFTS_REQUIRED);
    }
    @Test
    @DisplayName("✅ 배달부 캐릭터 설정 성공")
    void updateDeliveryCharacter_성공() {
        // Given
        Long bundleId = 1L;
        BundleRequest request = new BundleRequest();
        request.setDeliveryCharacterType(DeliveryCharacterType.CHARACTER_1);

        Bundle mockBundle = Bundle.builder()
                .id(bundleId)
                .userId(mockUser.getId())
                .name("Test Bundle")
                .designType(DesignType.RED)
                .status(BundleStatus.DRAFT)
                .isRead(false)
                .build();

        when(bundleRepository.findByIdAndUserId(bundleId, mockUser.getId()))
                .thenReturn(Optional.of(mockBundle));

        // When
        BundleResponse response = bundleService.updateDeliveryCharacter(bundleId, request);

        // Then
        assertThat(response.getDeliveryCharacterType()).isEqualTo(DeliveryCharacterType.CHARACTER_1);
        assertThat(response.getStatus()).isEqualTo(BundleStatus.PUBLISHED);
    }

    @Test
    @DisplayName("❌ 배달부 캐릭터 설정 실패 - 보따리를 찾을 수 없음")
    void updateDeliveryCharacter_실패_보따리없음() {
        // Given
        Long bundleId = 999L;
        BundleRequest request = new BundleRequest();
        request.setDeliveryCharacterType(DeliveryCharacterType.CHARACTER_1);

        when(bundleRepository.findByIdAndUserId(bundleId, mockUser.getId()))
                .thenReturn(Optional.empty());

        // When & Then
        BaseException exception = assertThrows(BaseException.class,
                () -> bundleService.updateDeliveryCharacter(bundleId, request));

        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_NOT_FOUND);
    }

    @Test
    @DisplayName("❌ 배달부 캐릭터 설정 실패 - 이미 배달 시작된 보따리")
    void updateDeliveryCharacter_실패_이미배달시작() {
        // Given
        Long bundleId = 1L;
        BundleRequest request = new BundleRequest();
        request.setDeliveryCharacterType(DeliveryCharacterType.CHARACTER_1);

        Bundle mockBundle = Bundle.builder()
                .id(bundleId)
                .userId(mockUser.getId())
                .name("Test Bundle")
                .designType(DesignType.RED)
                .status(BundleStatus.PUBLISHED) // 이미 PUBLISHED 상태
                .deliveryCharacterType(DeliveryCharacterType.CHARACTER_2)
                .isRead(false)
                .build();

        when(bundleRepository.findByIdAndUserId(bundleId, mockUser.getId()))
                .thenReturn(Optional.of(mockBundle));

        // When & Then
        BaseException exception = assertThrows(BaseException.class,
                () -> bundleService.updateDeliveryCharacter(bundleId, request));

        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.INVALID_BUNDLE_STATUS);
    }
}
