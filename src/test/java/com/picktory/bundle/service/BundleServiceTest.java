package com.picktory.bundle.service;

import com.picktory.domain.bundle.dto.BundleCreateRequest;
import com.picktory.domain.bundle.dto.BundleResponse;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.bundle.service.BundleService;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.service.UserService;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.support.config.jwt.TestJwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BundleServiceTest {

    @Mock
    private BundleRepository bundleRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private BundleService bundleService;

    // ✅ 공통 JWT 사용
    private static final String TEST_JWT_TOKEN = TestJwtTokenProvider.generateTestToken("1");

    @Test
    void 보따리_최초생성_테스트() {
        // ✅ Given (Mock 유저 생성)
        User mockUser = User.builder()
                .kakaoId("testKakaoId")
                .nickname("TestUser")
                .build();

        when(userService.getCurrentActiveUser()).thenReturn(mockUser);

        // ✅ Given (보따리 생성 요청)
        BundleCreateRequest request = new BundleCreateRequest();
        request.setName("Test Bundle");
        request.setDesignType(DesignType.RED);
        request.setLink("http://test.com");

        // ✅ Given (Mock Bundle 저장)
        Bundle mockBundle = Bundle.builder()
                .id(1L)
                .userId(1L)
                .name(request.getName())
                .designType(request.getDesignType())
                .deliveryCharacterType(null)
                .link(request.getLink())
                .status(BundleStatus.DRAFT)
                .isRead(false)
                .build();

        when(bundleRepository.save(any(Bundle.class))).thenReturn(mockBundle);

        // ✅ When (보따리 생성)
        BundleResponse response = bundleService.createBundle(request);

        // ✅ Then (검증)
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Bundle");
        assertThat(response.getDesignType()).isEqualTo(request.getDesignType());
        assertThat(response.getStatus()).isEqualTo(BundleStatus.DRAFT);

        // ✅ 테스트용 JWT 출력
        System.out.println("✅ 목번들주소: " + mockBundle);
        System.out.println("✅ 테스트용 JWT: " + TEST_JWT_TOKEN);
    }
}
