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
import com.picktory.domain.gift.enums.GiftResponseTag;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import com.picktory.domain.response.dto.ResponseBundleDto;
import com.picktory.domain.response.dto.SaveGiftResponsesRequest;
import com.picktory.domain.response.dto.SaveGiftResponsesResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                .responseTag(GiftResponseTag.GREAT)
                .isResponsed(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private GiftImage createTestGiftImage(Long id, Long giftId, String imageUrl, boolean isPrimary) {
        return GiftImage.builder()
                .id(id)
                .giftId(giftId)
                .imageUrl(imageUrl)
                .isPrimary(isPrimary)
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    private SaveGiftResponsesRequest createTestRequest(Long bundleId, List<Long> giftIds) {
        List<SaveGiftResponsesRequest.GiftResponse> giftResponses = giftIds.stream()
                .map(giftId -> {
                    SaveGiftResponsesRequest.GiftResponse giftResponse = new SaveGiftResponsesRequest.GiftResponse();
                    giftResponse.setGiftId(giftId);
                    giftResponse.setResponseTag("GREAT");
                    return giftResponse;
                })
                .collect(Collectors.toList());

        SaveGiftResponsesRequest request = new SaveGiftResponsesRequest();
        request.setBundleId(bundleId.toString());
        request.setGifts(giftResponses);
        return request;
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
            List<GiftImage> giftImages = List.of(
                    createTestGiftImage(1L, gift.getId(), "http://example.com/image1.jpg", true),
                    createTestGiftImage(2L, gift.getId(), "http://example.com/image2.jpg", false)
            );
            List<Response> responses = List.of();

            when(bundleRepository.findByLink(link)).thenReturn(Optional.of(bundle));
            when(giftRepository.findByBundleId(bundle.getId())).thenReturn(List.of(gift));
            when(giftImageRepository.findByGiftIdIn(any())).thenReturn(giftImages);
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
                            assertThat(giftInfo.getMessage()).isNull(); // 응답되지 않은 선물이므로 메시지는 null
                            assertThat(giftInfo.getImageUrls())
                                    .hasSize(1)
                                    .containsExactly("http://example.com/image2.jpg"); // primary가 아닌 이미지만 포함
                            assertThat(giftInfo.getThumbnail())
                                    .isEqualTo("http://example.com/image1.jpg"); // primary 이미지가 thumbnail이 됨
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

    @Nested
    @DisplayName("선물 답변 저장")
    class SaveGiftResponses {
        @Test
        @DisplayName("모든 선물에 대한 답변을 정상적으로 저장할 수 있다")
        void success() {
            // given
            String link = "valid-link";
            Long bundleId = 1L;
            List<Long> giftIds = List.of(1L, 2L);

            Bundle bundle = createTestBundle(bundleId, link, BundleStatus.PUBLISHED);
            List<Gift> gifts = giftIds.stream()
                    .map(id -> createTestGift(id, bundleId))
                    .toList();
            SaveGiftResponsesRequest request = createTestRequest(bundleId, giftIds);

            when(bundleRepository.findByLink(link)).thenReturn(Optional.of(bundle));
            when(giftRepository.findByBundleId(bundleId)).thenReturn(gifts);
            when(responseRepository.existsByGiftIdIn(giftIds)).thenReturn(false);
            when(bundleRepository.save(any(Bundle.class))).thenReturn(bundle);
            when(responseRepository.saveAll(any())).thenReturn(List.of());

            // when
            SaveGiftResponsesResponse response = responseService.saveGiftResponses(link, request);

            // then
            assertThat(response.getAnsweredCount()).isEqualTo(giftIds.size());
            assertThat(response.getTotalCount()).isEqualTo(giftIds.size());

            verify(bundleRepository).findByLink(link);
            verify(giftRepository).findByBundleId(bundleId);
            verify(responseRepository).existsByGiftIdIn(giftIds);
            verify(responseRepository).saveAll(any());
            verify(bundleRepository).save(any(Bundle.class));
        }
    }
        @Test
        @DisplayName("이미 완료된 보따리에 답변을 저장할 수 없다")
        void fail_whenAlreadyCompleted() {
            // given
            String link = "completed-link";
            Bundle bundle = createTestBundle(1L, link, BundleStatus.COMPLETED);
            SaveGiftResponsesRequest request = createTestRequest(bundle.getId(), List.of(1L));

            when(bundleRepository.findByLink(link)).thenReturn(Optional.of(bundle));

            // when & then
            assertThatThrownBy(() -> responseService.saveGiftResponses(link, request))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.ALREADY_ANSWERED);
        }

        @Test
        @DisplayName("존재하지 않는 선물에 대한 답변을 저장할 수 없다")
        void fail_whenInvalidGiftId() {
            // given
            String link = "valid-link";
            Long bundleId = 1L;
            List<Long> requestGiftIds = List.of(999L); // 존재하지 않는 선물 ID

            Bundle bundle = createTestBundle(bundleId, link, BundleStatus.PUBLISHED);
            List<Gift> gifts = List.of(createTestGift(1L, bundleId)); // 실제 존재하는 선물
            SaveGiftResponsesRequest request = createTestRequest(bundleId, requestGiftIds);

            when(bundleRepository.findByLink(link)).thenReturn(Optional.of(bundle));
            when(giftRepository.findByBundleId(bundleId)).thenReturn(gifts);

            // when & then
            assertThatThrownBy(() -> responseService.saveGiftResponses(link, request))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INVALID_GIFT_ID);
        }

        @Test
        @DisplayName("이미 답변이 있는 선물에 대해 답변을 저장할 수 없다")
        void fail_whenResponseAlreadyExists() {
            // given
            String link = "valid-link";
            Long bundleId = 1L;
            List<Long> giftIds = List.of(1L);

            Bundle bundle = createTestBundle(bundleId, link, BundleStatus.PUBLISHED);
            List<Gift> gifts = List.of(createTestGift(1L, bundleId));
            SaveGiftResponsesRequest request = createTestRequest(bundleId, giftIds);

            when(bundleRepository.findByLink(link)).thenReturn(Optional.of(bundle));
            when(giftRepository.findByBundleId(bundleId)).thenReturn(gifts);
            when(responseRepository.existsByGiftIdIn(any())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> responseService.saveGiftResponses(link, request))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.ALREADY_ANSWERED);
        }

        @Test
        @DisplayName("모든 선물에 대한 답변이 없으면 저장할 수 없다")
        void fail_whenIncompleteResponses() {
            // given
            String link = "valid-link";
            Long bundleId = 1L;
            List<Long> giftIds = List.of(1L); // 1개의 선물만 답변

            Bundle bundle = createTestBundle(bundleId, link, BundleStatus.PUBLISHED);
            List<Gift> gifts = List.of(
                    createTestGift(1L, bundleId),
                    createTestGift(2L, bundleId) // 2개의 선물이 존재
            );
            SaveGiftResponsesRequest request = createTestRequest(bundleId, giftIds);

            when(bundleRepository.findByLink(link)).thenReturn(Optional.of(bundle));
            when(giftRepository.findByBundleId(bundleId)).thenReturn(gifts);

            // when & then
            assertThatThrownBy(() -> responseService.saveGiftResponses(link, request))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("status", BaseResponseStatus.INCOMPLETE_RESPONSES);
        }
    }
