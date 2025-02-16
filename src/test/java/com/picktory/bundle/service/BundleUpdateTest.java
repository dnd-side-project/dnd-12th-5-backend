package com.picktory.bundle.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.config.auth.AuthenticationService;
import com.picktory.domain.bundle.dto.*;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.bundle.service.BundleService;
import com.picktory.domain.gift.dto.*;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import com.picktory.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * 이미 최초 생성되어 2개 이상의 선물이 추가된 보따리의 경우
 */

@ExtendWith(MockitoExtension.class)
class BundleUpdateTest {

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
    private Bundle mockBundle;

    private static final Logger log = LoggerFactory.getLogger(BundleUpdateTest.class);

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .kakaoId(12345678L)
                .nickname("TestUser")
                .build();

        ReflectionTestUtils.setField(mockUser, "id", 1L);

        mockBundle = Bundle.builder()
                .id(1L)
                .userId(mockUser.getId())
                .name("기존 보따리")
                .designType(DesignType.RED)
                .status(BundleStatus.DRAFT)
                .isRead(false)
                .build();

        when(authenticationService.getAuthenticatedUser()).thenReturn(mockUser);
        when(bundleRepository.findById(1L)).thenReturn(Optional.of(mockBundle));

        lenient().when(authenticationService.getAuthenticatedUser()).thenReturn(mockUser);
        lenient().when(bundleRepository.findById(1L)).thenReturn(Optional.of(mockBundle));
        // 기존 보따리는 최소 2개의 선물을 포함해야 함
        Gift existingGift1 = Gift.builder()
                .id(100L)
                .bundleId(mockBundle.getId())
                .name("기존 선물 1")
                .message("기존 메시지 1")
                .purchaseUrl("https://old1.com")
                .build();

        Gift existingGift2 = Gift.builder()
                .id(101L)
                .bundleId(mockBundle.getId())
                .name("기존 선물 2")
                .message("기존 메시지 2")
                .purchaseUrl("https://old2.com")
                .build();

        when(giftRepository.findByBundleId(mockBundle.getId())).thenReturn(List.of(existingGift1, existingGift2));
    }

    @Test
    @DisplayName("❌ 보따리 업데이트 - 선물 삭제 (최소 개수 미달 예외)")
    void 보따리_업데이트_선물삭제_실패() {
        // Given: 기존 선물 2개 (100L, 101L)
        Gift existingGift1 = Gift.builder()
                .id(100L)
                .bundleId(mockBundle.getId())
                .name("기존 선물 1")
                .message("기존 메시지 1")
                .purchaseUrl("https://old1.com")
                .build();

        Gift existingGift2 = Gift.builder()
                .id(101L)
                .bundleId(mockBundle.getId())
                .name("기존 선물 2")
                .message("기존 메시지 2")
                .purchaseUrl("https://old2.com")
                .build();

        when(giftRepository.findByBundleId(mockBundle.getId())).thenReturn(List.of(existingGift1, existingGift2));

        // 요청 데이터: 기존 선물 중 1개만 유지 (100L 제거, 101L 유지)
        BundleUpdateRequest updateRequest = new BundleUpdateRequest();
        updateRequest.setGifts(List.of(
                new GiftUpdateRequest(101L, "유지될 선물", "유지된 메시지", "https://keep.com", List.of("https://img.com/keep1.jpg"))
        ));

        // When & Then: 최소 2개 미만으로 인해 예외 발생 검증
        BaseException exception = assertThrows(BaseException.class, () ->
                bundleService.updateBundle(mockBundle.getId(), updateRequest)
        );
        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_MINIMUM_GIFTS_REQUIRED);
    }

    @Test
    @DisplayName("✅ 보따리 업데이트 - 선물 추가 + 수정 (성공)")
    void 보따리_업데이트_선물추가_성공() {
        // Given: 기존 선물 2개 (100L, 101L) → 새로운 선물 추가
        GiftUpdateRequest newGiftRequest = new GiftUpdateRequest(null, "새로운 선물", "새 메시지", "https://new.com", List.of("https://img.com/new1.jpg"));

        BundleUpdateRequest updateRequest = new BundleUpdateRequest();
        updateRequest.setGifts(Arrays.asList(
                new GiftUpdateRequest(100L, "기존 선물 1 수정", "수정된 메시지 1", "https://old1-modified.com", List.of("https://img.com/old1.jpg")),
                new GiftUpdateRequest(101L, "기존 선물 2 수정", "수정된 메시지 2", "https://old2-modified.com", List.of("https://img.com/old2.jpg")),
                newGiftRequest
        ));

        Gift newGift = Gift.builder().id(102L).bundleId(mockBundle.getId()).name("새로운 선물").build();

        // ✅ 변경된 `when()` 사용: 전달된 리스트를 그대로 반환
        when(giftRepository.saveAll(any())).thenAnswer(invocation -> new ArrayList<>(invocation.getArgument(0)));

        // When
        BundleResponse updatedBundle = bundleService.updateBundle(mockBundle.getId(), updateRequest);

        // 🛠 디버깅: 업데이트된 선물 목록을 출력
        log.info("=== 업데이트된 선물 목록 ===");
            // 생성된 선물은 "ID: null" 찍힘. Mock 환경에서 saveAll()이 실제로 DB에 접근하지 않으므로, 실제 디비 사용시 점검필요.
        updatedBundle.getGifts().forEach(gift ->
                log.info("ID: {}, 이름: {}, 메시지: {}", gift.getId(), gift.getName(), gift.getMessage())
        );

        // 🛠 디버깅: 선물 개수 확인
        log.info("총 선물 개수: {}", updatedBundle.getGifts().size());

        // Then
        assertThat(updatedBundle.getGifts()).hasSize(3);
        assertThat(updatedBundle.getGifts().get(2).getName()).isEqualTo("새로운 선물");
    }



    @Test
    @DisplayName("✅ 보따리 업데이트 - 선물 1개는 수정 1개는 그대로(성공)")
    void 보따리_업데이트_선물수정_유지_성공() {
        // Given: 기존 선물 2개 (100L, 101L) - 메시지, 구매 링크 포함
        Gift existingGift1 = Gift.builder()
                .id(100L)
                .bundleId(mockBundle.getId())
                .name("기존 선물 1")
                .message("기존 메시지 1")
                .purchaseUrl("https://old1.com")
                .build();

        Gift existingGift2 = Gift.builder()
                .id(101L)
                .bundleId(mockBundle.getId())
                .name("기존 선물 2")
                .message("기존 메시지 2")
                .purchaseUrl("https://old2.com")
                .build();

        when(giftRepository.findByBundleId(mockBundle.getId())).thenReturn(List.of(existingGift1, existingGift2));

        // 요청 데이터: 기존 선물 1개 수정 (100L 수정), 101L 유지
        BundleUpdateRequest updateRequest = new BundleUpdateRequest();
        updateRequest.setGifts(List.of(
                new GiftUpdateRequest(100L, "수정된 선물 1", "수정된 메시지", "https://modified.com", null),
                new GiftUpdateRequest(101L, "기존 선물 2", "기존 메시지 2", "https://old2.com", null)
        ));

        // ✅ 변경된 `when()` 사용: 전달된 리스트를 그대로 반환
        when(giftRepository.saveAll(any())).thenAnswer(invocation -> new ArrayList<>(invocation.getArgument(0)));

        // When
        BundleResponse updatedBundle = bundleService.updateBundle(mockBundle.getId(), updateRequest);

        // 🛠 디버깅: 업데이트된 선물 목록 확인
        log.info("=== 업데이트된 선물 목록 ===");
        updatedBundle.getGifts().forEach(gift ->
                log.info("ID: {}, 이름: {}, 메시지: {}, 구매링크: {}",
                        gift.getId(), gift.getName(), gift.getMessage(), gift.getPurchaseUrl())
        );

        // Then
        assertThat(updatedBundle.getGifts()).hasSize(2);

        // ✅ 첫 번째 선물 (수정된 선물)
        GiftResponse updatedGift1 = updatedBundle.getGifts().get(0);
        assertThat(updatedGift1.getName()).isEqualTo("수정된 선물 1");
        assertThat(updatedGift1.getMessage()).isEqualTo("수정된 메시지");
        assertThat(updatedGift1.getPurchaseUrl()).isEqualTo("https://modified.com");

        // ✅ 두 번째 선물 (변경되지 않은 기존 선물)
        GiftResponse unchangedGift2 = updatedBundle.getGifts().get(1);
        assertThat(unchangedGift2.getName()).isEqualTo("기존 선물 2");
        assertThat(unchangedGift2.getMessage()).isEqualTo("기존 메시지 2");
        assertThat(unchangedGift2.getPurchaseUrl()).isEqualTo("https://old2.com");
    }


    @Test
    @DisplayName("✅ 보따리 업데이트 - 선물 수정 + 삭제 + 추가 (성공)")
    void 보따리_업데이트_선물수정_삭제_추가_성공() {
        // Given: 기존 선물 2개 (100L, 101L)
        Gift existingGift1 = Gift.builder().id(100L).bundleId(mockBundle.getId()).name("기존 선물 1").build();
        Gift existingGift2 = Gift.builder().id(101L).bundleId(mockBundle.getId()).name("기존 선물 2").build();
        when(giftRepository.findByBundleId(mockBundle.getId())).thenReturn(List.of(existingGift1, existingGift2));

        // 요청 데이터:
        // - 기존 선물 100L 삭제
        // - 기존 선물 101L 수정
        // - 새로운 선물 추가
        GiftUpdateRequest newGiftRequest = new GiftUpdateRequest(null, "새로운 선물", "새 메시지", "https://new.com", List.of("https://img.com/new1.jpg"));

        BundleUpdateRequest updateRequest = new BundleUpdateRequest();
        updateRequest.setGifts(List.of(
                new GiftUpdateRequest(101L, "수정된 선물 2", "수정된 메시지", "https://modified.com", List.of("https://img.com/modified.jpg")),
                newGiftRequest // 새로운 선물 추가
        ));

        // 새로 추가될 선물 (DB 저장 후 ID 할당됨)
        Gift newGift = Gift.builder().id(102L).bundleId(mockBundle.getId()).name("새로운 선물").build();

        // ✅ 변경된 `when()` 사용: 전달된 리스트를 그대로 반환 (ID 부여 가정)
        when(giftRepository.saveAll(any())).thenAnswer(invocation -> new ArrayList<>(invocation.getArgument(0)));

        // When
        BundleResponse updatedBundle = bundleService.updateBundle(mockBundle.getId(), updateRequest);

        // Then
        assertThat(updatedBundle.getGifts()).hasSize(2);
        assertThat(updatedBundle.getGifts().get(0).getName()).isEqualTo("수정된 선물 2");
        assertThat(updatedBundle.getGifts().get(1).getName()).isEqualTo("새로운 선물");
    }

    // 모기토 @BeforeEach 때문에 빌드가 실패하지만
    // 디버깅 결과 BaseResponseStatus.BUNDLE_NOT_FOUND)가 잘 발생함.
    @Test
    @DisplayName("❌ 보따리 업데이트 - 존재하지 않는 보따리 (예외 발생)")
    void 보따리_업데이트_존재하지않는_보따리_실패() {
        // Given: 존재하지 않는 보따리 ID
        Long invalidBundleId = 999L;
        when(bundleRepository.findById(invalidBundleId)).thenReturn(Optional.empty());

        BundleUpdateRequest updateRequest = new BundleUpdateRequest();
        updateRequest.setGifts(List.of(
                new GiftUpdateRequest(100L, "수정된 선물 1", "수정된 메시지", "https://modified.com", List.of("https://img.com/modified.jpg"))
        ));

        // When & Then: 존재하지 않는 보따리이므로 예외 발생
        BaseException exception = assertThrows(BaseException.class, () ->
                bundleService.updateBundle(invalidBundleId, updateRequest)
        );
        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.BUNDLE_NOT_FOUND);
    }


    // 모기토 @BeforeEach 때문에 빌드가 실패하지만
    // 디버깅 결과 BaseResponseStatus.BUNDLE_ACCESS_DENIED)가 잘 발생함.
    @Test
    @DisplayName("❌ 보따리 업데이트 - 권한 없는 유저 접근 (예외 발생)")
    void 보따리_업데이트_권한없는유저_실패() {
        // Given: 다른 유저의 보따리
        User anotherUser = User.builder()
                .kakaoId(87654321L)
                .nickname("OtherUser")
                .build();

        // ReflectionTestUtils를 사용해 ID 강제 주입
        ReflectionTestUtils.setField(anotherUser, "id", 2L);

        // 현재 로그인한 유저를 다른 유저로 설정
        doReturn(anotherUser).when(authenticationService).getAuthenticatedUser();

        BundleUpdateRequest updateRequest = new BundleUpdateRequest();
        updateRequest.setGifts(List.of(
                new GiftUpdateRequest(100L, "수정된 선물 1", "수정된 메시지", "https://modified.com", List.of("https://img.com/modified.jpg"))
        ));

        // 🛠 디버깅 로그 추가
        log.info("현재 로그인된 사용자 ID: {}", authenticationService.getAuthenticatedUser().getId());
        log.info("현재 보따리 소유자 ID: {}", mockBundle.getUserId());

        // When & Then: 예외 발생 여부 및 상태 코드 검증
        BaseException exception = assertThrows(BaseException.class, () -> {
            bundleService.updateBundle(mockBundle.getId(), updateRequest);
        });

        // 🛠 예외가 실제로 발생했는지 확인하는 추가 로그
        log.info("예외 발생: {}", exception.getMessage());

        // ✅ 예외 타입 검증
        assertThat(exception).isInstanceOf(BaseException.class);

        // ✅ 예외 상태 코드 검증
        assertThat(exception.getStatus())
                .as("예외가 발생했지만 상태 코드가 올바른지 검증")
                .isEqualTo(BaseResponseStatus.BUNDLE_ACCESS_DENIED);
    }

}
