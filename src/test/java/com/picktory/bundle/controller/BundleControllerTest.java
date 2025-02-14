package com.picktory.bundle.controller;

import com.picktory.domain.bundle.controller.BundleController;
import com.picktory.domain.bundle.dto.BundleListResponse;
import com.picktory.domain.bundle.dto.BundleMainListResponse;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.bundle.service.BundleService;
import com.picktory.common.BaseResponse;
import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)  // ✅ Mockito 확장 적용
class BundleControllerTest {

    @Mock
    private BundleService bundleService;  // ✅ @Mock 사용

    @InjectMocks
    private BundleController bundleController;  // ✅ 컨트롤러에 Mock 객체 주입

    private List<BundleListResponse> mockBundles;
    private List<BundleMainListResponse> mockMainBundles;

    @BeforeEach
    void setUp() {
        mockBundles = List.of(
                BundleListResponse.builder()
                        .id(1L)
                        .name("테스트 보따리 1")
                        .designType(DesignType.RED)
                        .updatedAt(LocalDateTime.of(2024, 2, 7, 12, 0))
                        .status(BundleStatus.COMPLETED)
                        .isRead(false)  // ✅ COMPLETED 상태일 때 초기값 false
                        .build(),
                BundleListResponse.builder()
                        .id(2L)
                        .name("테스트 보따리 2")
                        .designType(DesignType.GREEN)
                        .updatedAt(LocalDateTime.of(2024, 2, 6, 14, 30))
                        .status(BundleStatus.DRAFT)
                        .isRead(false)
                        .build()
        );
        mockMainBundles = IntStream.range(1, 11)
                .mapToObj(i -> BundleMainListResponse.builder()
                        .id((long) i)
                        .name("테스트 보따리 " + i)
                        .designType(i % 2 == 0 ? DesignType.RED : DesignType.GREEN)
                        .updatedAt(LocalDateTime.of(2024, 2, 15, 12, 0).minusDays(i)) // 날짜가 최신순
                        .build())
                .sorted(Comparator.comparing(BundleMainListResponse::getUpdatedAt).reversed()) // 최신순 정렬
                .toList();
    }

    /**
     * ✅ 보따리 목록 조회 성공 테스트
     */
    @Test
    void 보따리_목록_조회_성공() {
        // Given
        when(bundleService.getUserBundles()).thenReturn(mockBundles);

        // When
        ResponseEntity<BaseResponse<List<BundleListResponse>>> response = bundleController.getBundles();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResult()).hasSize(2);

        // ✅ 응답 필드 검증
        BundleListResponse firstBundle = response.getBody().getResult().get(0);
        assertThat(firstBundle.getId()).isEqualTo(1L);
        assertThat(firstBundle.getName()).isEqualTo("테스트 보따리 1");
        assertThat(firstBundle.getDesignType()).isEqualTo(DesignType.RED);
        assertThat(firstBundle.getUpdatedAt()).isEqualTo(LocalDateTime.of(2024, 2, 7, 12, 0));
        assertThat(firstBundle.getStatus()).isEqualTo(BundleStatus.COMPLETED);
        assertThat(firstBundle.getIsRead()).isFalse();  // ✅ COMPLETED 상태의 isRead 초기값 검증

        verify(bundleService, times(1)).getUserBundles();
    }

    /**
     * ✅ 보따리 메인 목록 조회 성공 테스트 (최신 8개만 반환)
     */
    @Test
    void 보따리_메인_목록_조회_성공() {
        // Given
        when(bundleService.getUserMainBundles()).thenReturn(mockMainBundles.subList(0, 8)); // 8개만 반환

        // When
        ResponseEntity<BaseResponse<List<BundleMainListResponse>>> response = bundleController.getMainBundles();

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResult()).hasSize(8); // ✅ 최신 8개만 반환되는지 확인

        // ✅ 응답 필드 검증 (최신 순서)
        List<BundleMainListResponse> resultBundles = response.getBody().getResult();
        for (int i = 0; i < resultBundles.size(); i++) {
            assertThat(resultBundles.get(i).getId()).isEqualTo(mockMainBundles.get(i).getId());
            assertThat(resultBundles.get(i).getName()).isEqualTo(mockMainBundles.get(i).getName());
            assertThat(resultBundles.get(i).getDesignType()).isEqualTo(mockMainBundles.get(i).getDesignType());
            assertThat(resultBundles.get(i).getUpdatedAt()).isEqualTo(mockMainBundles.get(i).getUpdatedAt());
        }

        verify(bundleService, times(1)).getUserMainBundles();
    }

    /**
     * ✅ 401 Unauthorized - 인증 실패
     */
    @Test
    void 보따리_목록_조회_실패_인증_없음() {
        // Given
        when(bundleService.getUserBundles()).thenThrow(new BaseException(BaseResponseStatus.INVALID_JWT));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> bundleController.getBundles());
        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.INVALID_JWT);
        assertThat(exception.getStatus().getCode()).isEqualTo(401);
        assertThat(exception.getStatus().getMessage()).isEqualTo("유효하지 않은 JWT입니다.");
    }

    /**
     * ✅ 403 Forbidden - 본인 보따리만 조회 가능
     */
    @Test
    void 보따리_목록_조회_실패_권한_없음() {
        // Given
        when(bundleService.getUserBundles()).thenThrow(new BaseException(BaseResponseStatus.FORBIDDEN));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> bundleController.getBundles());
        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.FORBIDDEN);
        assertThat(exception.getStatus().getCode()).isEqualTo(403);
        assertThat(exception.getStatus().getMessage()).isEqualTo("이 리소스에 대한 접근 권한이 없습니다.");
    }

    /**
     * ✅ 500 Internal Server Error - 서버 오류
     */
    @Test
    void 보따리_목록_조회_실패_서버_오류() {
        // Given
        when(bundleService.getUserBundles()).thenThrow(new BaseException(BaseResponseStatus.INTERNAL_SERVER_ERROR));

        // When & Then
        BaseException exception = assertThrows(BaseException.class, () -> bundleController.getBundles());
        assertThat(exception.getStatus()).isEqualTo(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getStatus().getCode()).isEqualTo(500);
        assertThat(exception.getStatus().getMessage()).isEqualTo("서버에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }
}
