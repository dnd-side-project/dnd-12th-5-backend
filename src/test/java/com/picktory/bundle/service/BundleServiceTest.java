//package com.picktory.bundle.service;
//
//import com.picktory.common.BaseResponseStatus;
//import com.picktory.common.exception.BaseException;
//import com.picktory.config.auth.AuthenticationService;
//import com.picktory.domain.bundle.dto.BundleDeliveryRequest;
//import com.picktory.domain.bundle.dto.BundleRequest;
//import com.picktory.domain.bundle.dto.BundleResponse;
//import com.picktory.domain.bundle.enums.DesignType;
//import com.picktory.domain.bundle.service.BundleService;
//import com.picktory.domain.bundle.repository.BundleRepository;
//import com.picktory.domain.bundle.entity.Bundle;
//import com.picktory.domain.bundle.enums.BundleStatus;
//import com.picktory.domain.bundle.enums.DeliveryCharacterType;
//import com.picktory.domain.gift.dto.DraftGiftsResponse;
//import com.picktory.domain.gift.dto.GiftDetailResponse;
//import com.picktory.domain.gift.dto.GiftRequest;
//import com.picktory.domain.gift.entity.Gift;
//import com.picktory.domain.gift.entity.GiftImage;
//import com.picktory.domain.gift.repository.GiftImageRepository;
//import com.picktory.domain.gift.repository.GiftRepository;
//import com.picktory.domain.user.entity.User;
//import com.picktory.support.config.jwt.TestJwtTokenProvider;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import static org.mockito.Mockito.verify;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class BundleServiceTest {
//
//    @Mock
//    private BundleRepository bundleRepository;
//
//    @Mock
//    private GiftRepository giftRepository;
//
//    @Mock
//    private GiftImageRepository giftImageRepository;
//
//    @Mock
//    private AuthenticationService authenticationService;
//
//    @InjectMocks
//    private BundleService bundleService;
//
//    private User mockUser;
//
//    @BeforeEach
//    void setUp() {
//        mockUser = User.builder()
//                .kakaoId(12345678L)
//                .nickname("TestUser")
//                .build();
//
//        // ✅ ID를 명확히 설정- getId()가 null이 되지 않도록
//        ReflectionTestUtils.setField(mockUser, "id", 1L);
//
//        when(authenticationService.getAuthenticatedUser()).thenReturn(mockUser);
//    }
//
//
//    @Test
//    @DisplayName("✅ 보따리 최초 생성 성공")
//    void 보따리_최초생성_테스트() {
//        // Given (선물 목록 생성)
//        GiftRequest giftRequest1 = new GiftRequest();
//        giftRequest1.setMessage("첫 번째 선물 메시지");
//        giftRequest1.setImageUrls(List.of("http://image1.com"));
//
//        GiftRequest giftRequest2 = new GiftRequest();
//        giftRequest2.setMessage("두 번째 선물 메시지");
//        giftRequest2.setImageUrls(List.of("http://image2.com"));
//
//        List<GiftRequest> giftRequests = Arrays.asList(giftRequest1, giftRequest2);
//
//        // Given (보따리 생성 요청)
//        BundleRequest request = new BundleRequest();
//        request.setName("Test Bundle");
//        request.setDesignType(DesignType.RED);
//        request.setGifts(giftRequests);
//
//        // Given (Mock Bundle 저장)
//        Bundle mockBundle = Bundle.builder()
//                .id(1L)
//                .userId(1L)
//                .name(request.getName())
//                .designType(request.getDesignType())
//                .status(BundleStatus.DRAFT)
//                .isRead(false)
//                .build();
//
//        when(bundleRepository.save(any(Bundle.class))).thenReturn(mockBundle);
//
//        // Given (Mock 선물 저장)
//        Gift mockGift1 = Gift.builder().id(100L).bundleId(mockBundle.getId()).message("첫 번째 선물 메시지").build();
//        Gift mockGift2 = Gift.builder().id(101L).bundleId(mockBundle.getId()).message("두 번째 선물 메시지").build();
//        when(giftRepository.saveAll(any())).thenReturn(List.of(mockGift1, mockGift2));
//
//        // Given (Mock 선물 이미지 저장)
//        GiftImage mockImage1 = GiftImage.builder().id(1000L).gift(mockGift1).imageUrl("http://image1.com").build();
//        GiftImage mockImage2 = GiftImage.builder().id(1001L).gift(mockGift2).imageUrl("http://image2.com").build();
//
//        when(giftImageRepository.saveAll(any())).thenReturn(List.of(mockImage1, mockImage2));
//
//        // When (보따리 생성)
//        BundleResponse response = bundleService.createBundle(request);
//
//        // Then (검증)
//        assertThat(response.getUserId()).isEqualTo(1L);
//        assertThat(response.getName()).isEqualTo("Test Bundle");
//        assertThat(response.getDesignType()).isEqualTo(request.getDesignType());
//        assertThat(response.getStatus()).isEqualTo(BundleStatus.DRAFT);
//        assertThat(response.getGifts()).hasSize(2);
//    }
//
//    @Test
//    @DisplayName("❌ 하루 최대 보따리 개수 초과")
//    void 보따리_생성_실패_하루최대개수초과() {
//        // Given
//        when(bundleRepository.countByUserIdAndCreatedAtAfter(any(Long.class), any(LocalDateTime.class)))
//                .thenReturn(10L); // 이미 10개 생성됨
//
//        BundleRequest request = new BundleRequest();
//        request.setName("Test Bundle");
//        request.setDesignType(DesignType.RED);
//        request.setGifts(Arrays.asList(new GiftRequest(), new GiftRequest()));
//
//        // When & Then
//        BaseException exception = assertThrows(BaseException.class, () -> bundleService.createBundle(request));
//        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_DAILY_LIMIT_EXCEEDED);
//    }
//
//    @Test
//    @DisplayName("❌ 보따리 이름 없음")
//    void 보따리_생성_실패_이름없음() {
//        // Given
//        BundleRequest request = new BundleRequest();
//        request.setName(""); // 빈 문자열
//        request.setDesignType(DesignType.RED);
//        request.setGifts(Arrays.asList(new GiftRequest(), new GiftRequest()));
//
//        // When & Then
//        BaseException exception = assertThrows(BaseException.class, () -> bundleService.createBundle(request));
//        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_NAME_REQUIRED);
//    }
//
//    @Test
//    @DisplayName("❌ 보따리 디자인 타입 없음")
//    void 보따리_생성_실패_디자인타입없음() {
//        // Given
//        BundleRequest request = new BundleRequest();
//        request.setName("Test Bundle");
//        request.setDesignType(null); // 디자인 타입 없음
//        request.setGifts(Arrays.asList(new GiftRequest(), new GiftRequest()));
//
//        // When & Then
//        BaseException exception = assertThrows(BaseException.class, () -> bundleService.createBundle(request));
//        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_DESIGN_REQUIRED);
//    }
//
//    @Test
//    @DisplayName("❌ 보따리에 선물이 2개 미만")
//    void 보따리_생성_실패_선물_2개미만() {
//        // Given
//        BundleRequest request = new BundleRequest();
//        request.setName("Test Bundle");
//        request.setDesignType(DesignType.RED);
//        request.setGifts(Collections.singletonList(new GiftRequest()));
//
//        // When & Then
//        BaseException exception = assertThrows(BaseException.class, () -> bundleService.createBundle(request));
//        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_MINIMUM_GIFTS_REQUIRED);
//    }
//    @Test
//    @DisplayName("✅ 배달부 캐릭터 설정 성공")
//    void updateDeliveryCharacter_성공() {
//        // Given
//        Long bundleId = 1L;
//        BundleDeliveryRequest request = new BundleDeliveryRequest(DeliveryCharacterType.CHARACTER_1);
//
//        Bundle mockBundle = Bundle.builder()
//                .id(bundleId)
//                .userId(mockUser.getId())
//                .name("Test Bundle")
//                .designType(DesignType.RED)
//                .status(BundleStatus.DRAFT)  // DRAFT 상태여야 함
//                .isRead(false)
//                .build();
//
//        when(bundleRepository.findById(bundleId))
//                .thenReturn(Optional.of(mockBundle));
//        when(bundleRepository.save(any(Bundle.class)))
//                .thenReturn(mockBundle);
//
//        // When
//        BundleResponse response = bundleService.updateDeliveryCharacter(bundleId, request);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.getDeliveryCharacterType()).isEqualTo(DeliveryCharacterType.CHARACTER_1);
//        assertThat(response.getStatus()).isEqualTo(BundleStatus.PUBLISHED);
//
//        verify(bundleRepository).findById(bundleId);
//        verify(bundleRepository).save(any(Bundle.class));
//    }
//
//    @Test
//    @DisplayName("❌ 배달부 캐릭터 설정 실패 - 보따리를 찾을 수 없음")
//    void updateDeliveryCharacter_실패_보따리없음() {
//        // Given
//        Long bundleId = 999L;
//        BundleDeliveryRequest request = new BundleDeliveryRequest(DeliveryCharacterType.CHARACTER_1);
//
//        when(bundleRepository.findById(bundleId))
//                .thenReturn(Optional.empty());
//
//        // When & Then
//        BaseException exception = assertThrows(BaseException.class,
//                () -> bundleService.updateDeliveryCharacter(bundleId, request));
//
//        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_NOT_FOUND);
//        verify(bundleRepository).findById(bundleId);
//    }
//    @Test
//    @DisplayName("❌ 배달부 캐릭터 설정 실패 - 이미 배달 시작된 보따리")
//    void updateDeliveryCharacter_실패_이미배달시작() {
//        // Given
//        Long bundleId = 1L;
//        BundleDeliveryRequest request = new BundleDeliveryRequest(DeliveryCharacterType.CHARACTER_1);
//
//        Bundle mockBundle = Bundle.builder()
//                .id(bundleId)
//                .userId(mockUser.getId())
//                .name("Test Bundle")
//                .designType(DesignType.RED)
//                .status(BundleStatus.PUBLISHED)  // 이미 PUBLISHED 상태
//                .deliveryCharacterType(DeliveryCharacterType.CHARACTER_2)
//                .link("/delivery/existing-link")
//                .isRead(false)
//                .build();
//
//        when(bundleRepository.findById(bundleId))
//                .thenReturn(Optional.of(mockBundle));
//
//        // When & Then
//        BaseException exception = assertThrows(BaseException.class,
//                () -> bundleService.updateDeliveryCharacter(bundleId, request));
//
//        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.INVALID_BUNDLE_STATUS);
//        verify(bundleRepository).findById(bundleId);
//    }
//
//    @Test
//    @DisplayName("✅ 개별 선물 조회 성공")
//    void getGift_성공() {
//        // Given
//        Long bundleId = 1L;
//        Long giftId = 1L;
//
//        Bundle mockBundle = Bundle.builder()
//                .id(bundleId)
//                .userId(mockUser.getId())
//                .name("Test Bundle")
//                .designType(DesignType.RED)
//                .status(BundleStatus.PUBLISHED)
//                .build();
//
//        Gift mockGift = Gift.builder()
//                .id(giftId)
//                .bundleId(bundleId)
//                .name("고급 초콜릿")
//                .message("특별한 날을 위한 달콤한 선물!")
//                .purchaseUrl("https://example.com/chocolate")
//                .build();
//
//        List<GiftImage> mockImages = Arrays.asList(
//                GiftImage.builder()
//                        .id(1L)
//                        .gift(mockGift)
//                        .imageUrl("https://s3.example.com/image1.jpg")
//                        .isPrimary(true)
//                        .build(),
//                GiftImage.builder()
//                        .id(2L)
//                        .gift(mockGift)
//                        .imageUrl("https://s3.example.com/image2.jpg")
//                        .isPrimary(false)
//                        .build()
//        );
//
//        when(bundleRepository.findById(bundleId)).thenReturn(Optional.of(mockBundle));
//        when(giftRepository.findByIdAndBundleId(giftId, bundleId)).thenReturn(Optional.of(mockGift));
//        when(giftImageRepository.findByGiftId(giftId)).thenReturn(mockImages);
//
//        // When
//        GiftDetailResponse response = bundleService.getGift(bundleId, giftId);
//
//        // Then
//        assertThat(response).isNotNull();
//        assertThat(response.getId()).isEqualTo(giftId);
//        assertThat(response.getName()).isEqualTo("고급 초콜릿");
//        assertThat(response.getMessage()).isEqualTo("특별한 날을 위한 달콤한 선물!");
//        assertThat(response.getPurchaseUrl()).isEqualTo("https://example.com/chocolate");
//        assertThat(response.getThumbnail()).isEqualTo("https://s3.example.com/image1.jpg");
//        assertThat(response.getImageUrls()).hasSize(1);
//        assertThat(response.getImageUrls().get(0)).isEqualTo("https://s3.example.com/image2.jpg");
//
//        verify(bundleRepository).findById(bundleId);
//        verify(giftRepository).findByIdAndBundleId(giftId, bundleId);
//        verify(giftImageRepository).findByGiftId(giftId);
//    }
//
//    @Test
//    @DisplayName("❌ 개별 선물 조회 실패 - 존재하지 않는 보따리")
//    void getGift_실패_보따리없음() {
//        // Given
//        Long nonExistentBundleId = 999L;
//        Long giftId = 1L;
//
//        when(bundleRepository.findById(nonExistentBundleId)).thenReturn(Optional.empty());
//
//        // When & Then
//        BaseException exception = assertThrows(BaseException.class,
//                () -> bundleService.getGift(nonExistentBundleId, giftId));
//
//        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_NOT_FOUND);
//        verify(bundleRepository).findById(nonExistentBundleId);
//    }
//
//    @Test
//    @DisplayName("❌ 개별 선물 조회 실패 - 존재하지 않는 선물")
//    void getGift_실패_선물없음() {
//        // Given
//        Long bundleId = 1L;
//        Long nonExistentGiftId = 999L;
//
//        Bundle mockBundle = Bundle.builder()
//                .id(bundleId)
//                .userId(mockUser.getId())
//                .name("Test Bundle")
//                .designType(DesignType.RED)
//                .status(BundleStatus.PUBLISHED)
//                .build();
//
//        when(bundleRepository.findById(bundleId)).thenReturn(Optional.of(mockBundle));
//        when(giftRepository.findByIdAndBundleId(nonExistentGiftId, bundleId)).thenReturn(Optional.empty());
//
//        // When & Then
//        BaseException exception = assertThrows(BaseException.class,
//                () -> bundleService.getGift(bundleId, nonExistentGiftId));
//
//        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.GIFT_NOT_FOUND);
//        verify(bundleRepository).findById(bundleId);
//        verify(giftRepository).findByIdAndBundleId(nonExistentGiftId, bundleId);
//    }
//
//    @Test
//    @DisplayName("❌ 개별 선물 조회 실패 - 권한 없음")
//    void getGift_실패_권한없음() {
//        // Given
//        Long bundleId = 1L;
//        Long giftId = 1L;
//        Long differentUserId = 999L;
//
//        Bundle mockBundle = Bundle.builder()
//                .id(bundleId)
//                .userId(differentUserId)  // 다른 사용자의 보따리
//                .name("Test Bundle")
//                .designType(DesignType.RED)
//                .status(BundleStatus.PUBLISHED)
//                .build();
//
//        when(bundleRepository.findById(bundleId)).thenReturn(Optional.of(mockBundle));
//
//        // When & Then
//        BaseException exception = assertThrows(BaseException.class,
//                () -> bundleService.getGift(bundleId, giftId));
//
//        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_ACCESS_DENIED);
//        verify(bundleRepository).findById(bundleId);
//    }
//
//    @Test
//    @DisplayName("✅ 임시 저장된 보따리의 선물 목록 조회 성공")
//    void getDraftGifts_성공() {
//        // Given
//        Long bundleId = 1L;
//        Bundle mockBundle = Bundle.builder()
//                .id(bundleId)
//                .userId(mockUser.getId())
//                .status(BundleStatus.DRAFT)
//                .build();
//
//        List<Gift> mockGifts = Arrays.asList(
//                Gift.builder().id(1L).bundleId(bundleId).name("향수").build(),
//                Gift.builder().id(2L).bundleId(bundleId).name("초콜릿").build()
//        );
//
//        List<GiftImage> mockImages = Arrays.asList(
//                GiftImage.builder().id(1L).gift(mockGifts.get(0)).isPrimary(true).build(),
//                GiftImage.builder().id(2L).gift(mockGifts.get(1)).isPrimary(true).build()
//        );
//
//        when(bundleRepository.findById(bundleId)).thenReturn(Optional.of(mockBundle));
//        when(giftRepository.findByBundleId(bundleId)).thenReturn(mockGifts);
//        when(giftImageRepository.findByGiftIds(any())).thenReturn(mockImages);
//
//        // When
//        DraftGiftsResponse response = bundleService.getDraftGifts(bundleId);
//
//        // Then
//        assertThat(response.getGifts()).hasSize(2);
//        verify(bundleRepository).findById(bundleId);
//        verify(giftRepository).findByBundleId(bundleId);
//    }
//
//    @Test
//    @DisplayName("❌ 임시 저장된 보따리의 선물 목록 조회 실패 - DRAFT 상태 아님")
//    void getDraftGifts_실패_DRAFT상태아님() {
//        // Given
//        Long bundleId = 1L;
//        Bundle mockBundle = Bundle.builder()
//                .id(bundleId)
//                .userId(mockUser.getId())
//                .status(BundleStatus.PUBLISHED)
//                .build();
//
//        when(bundleRepository.findById(bundleId)).thenReturn(Optional.of(mockBundle));
//
//        // When & Then
//        BaseException exception = assertThrows(BaseException.class,
//                () -> bundleService.getDraftGifts(bundleId));
//        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.INVALID_BUNDLE_STATUS);
//    }
//}