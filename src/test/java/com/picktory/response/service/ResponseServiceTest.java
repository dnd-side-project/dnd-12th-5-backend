package com.picktory.response.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.enums.DeliveryCharacterType;
import com.picktory.domain.bundle.enums.DesignType;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import com.picktory.domain.response.dto.ResponseBundleDto;
import com.picktory.domain.response.entity.Response;
import com.picktory.domain.response.repository.ResponseRepository;
import com.picktory.domain.response.service.ResponseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseServiceTest {

    @InjectMocks
    private ResponseService responseService;

    @Mock
    private BundleRepository bundleRepository;
    @Mock
    private GiftRepository giftRepository;
    @Mock
    private GiftImageRepository giftImageRepository;
    @Mock
    private ResponseRepository responseRepository;

    private Bundle createTestBundle(Long id, String link, BundleStatus status) {
        return Bundle.builder()
                .id(id)
                .userId(1L)
                .name("테스트 보따리")
                .designType(DesignType.RED)
                .deliveryCharacterType(DeliveryCharacterType.CHARACTER_1)
                .link(link)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .publishedAt(status == BundleStatus.PUBLISHED ? LocalDateTime.now() : null)
                .isRead(false)
                .build();
    }

    private Gift createTestGift(Long id, Long bundleId) {
        return Gift.builder()
                .id(id)
                .bundleId(bundleId)
                .name("테스트 선물")
                .message("테스트 메시지")
                .purchaseUrl("http://test.com")
                .isResponsed(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private GiftImage createTestGiftImage(Long giftId, boolean isPrimary) {
        return GiftImage.builder()
                .id(1L)
                .giftId(giftId)
                .imageUrl("http://example.com/image.jpg")
                .isPrimary(isPrimary)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("선물 보따리 조회")
    class GetBundle {

        @Test
        @DisplayName("PUBLISHED 상태의 보따리를 정상적으로 조회할 수 있다")
        void success() {
            // given
            String link = "valid-link";
            Bundle bundle = createTestBundle(1L, link, BundleStatus.PUBLISHED);
            Gift gift = createTestGift(1L, bundle.getId());
            GiftImage thumbnail = createTestGiftImage(gift.getId(), true);
            GiftImage additionalImage = createTestGiftImage(gift.getId(), false);
            List<Response> responses = List.of();

            when(bundleRepository.findByLink(link)).thenReturn(Optional.of(bundle));
            when(giftRepository.findByBundleId(bundle.getId())).thenReturn(List.of(gift));
            when(giftImageRepository.findByGiftIdIn(any())).thenReturn(List.of(thumbnail, additionalImage));
            when(responseRepository.findAllByBundleIdAndGiftIds(anyLong(), any())).thenReturn(responses);

            // when
            ResponseBundleDto result = responseService.getBundleByLink(link);

            // then
            assertThat(result.getBundle()).satisfies(bundleInfo -> {
                assertThat(bundleInfo.getDelivery_character_type())
                        .isEqualTo(DeliveryCharacterType.CHARACTER_1.name());
                assertThat(bundleInfo.getStatus())
                        .isEqualTo(BundleStatus.PUBLISHED.name());
                assertThat(bundleInfo.getTotal_gifts()).isEqualTo(1);
                assertThat(bundleInfo.getGifts()).hasSize(1)
                        .first()
                        .satisfies(giftInfo -> {
                            assertThat(giftInfo.getId()).isEqualTo(gift.getId());
                            assertThat(giftInfo.getMessage()).isNull();
                            assertThat(giftInfo.getThumbnail()).isNotNull();
                            assertThat(giftInfo.getImageUrls()).hasSize(1);
                        });
            });

            verify(bundleRepository).findByLink(link);
            verify(giftRepository).findByBundleId(bundle.getId());
            verify(giftImageRepository).findByGiftIdIn(any());
            verify(responseRepository).findAllByBundleIdAndGiftIds(anyLong(), any());
        }

        @Test
        @DisplayName("존재하지 않는 링크로 조회시 예외가 발생한다")
        void fail_whenInvalidLink() {
            // given
            String invalidLink = "invalid-link";
            when(bundleRepository.findByLink(invalidLink)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> responseService.getBundleByLink(invalidLink))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_LINK);
        }

        @Test
        @DisplayName("COMPLETED 상태의 보따리 조회시 예외가 발생한다")
        void fail_whenCompleted() {
            // given
            String link = "completed-link";
            Bundle bundle = createTestBundle(1L, link, BundleStatus.COMPLETED);
            when(bundleRepository.findByLink(link)).thenReturn(Optional.of(bundle));

            // when & then
            assertThatThrownBy(() -> responseService.getBundleByLink(link))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_BUNDLE_STATUS);
        }
    }
}