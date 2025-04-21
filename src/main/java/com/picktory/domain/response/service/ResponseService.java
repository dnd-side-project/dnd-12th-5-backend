package com.picktory.domain.response.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.enums.GiftResponseTag;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.response.dto.ResponseBundleDto;
import com.picktory.domain.response.dto.SaveGiftResponsesRequest;
import com.picktory.domain.response.dto.SaveGiftResponsesResponse;
import com.picktory.domain.response.entity.Response;
import com.picktory.domain.response.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseService {
    private final BundleRepository bundleRepository;
    private final GiftRepository giftRepository;
    private final GiftImageRepository giftImageRepository;
    private final ResponseRepository responseRepository;

    @Transactional(readOnly = true)
    public ResponseBundleDto getBundleByLink(String link) {
        Bundle bundle = findBundleByLink(link);
        validateBundleStatus(bundle);

        List<Gift> gifts = findGiftsByBundleId(bundle.getId());
        List<GiftImage> images = findGiftImages(gifts);
        List<Response> responses = findResponses(bundle.getId(), gifts);

        updateGiftResponseStatus(gifts, responses);

        return ResponseBundleDto.fromEntity(bundle, gifts, images);
    }

    @Transactional
    public SaveGiftResponsesResponse saveGiftResponses(String link, SaveGiftResponsesRequest request) {
        // 1. 번들 검증 및 조회
        Bundle bundle = validateAndGetBundle(link);

        // 2. 선물 목록 검증 및 조회
        List<Gift> gifts = validateAndGetGifts(bundle.getId(), request.getGifts());

        // 3. 기존 응답 없음 검증
        validateNoExistingResponses(gifts);

        // 4. 모든 선물에 대한 응답 여부 검증
        validateAllGiftsResponded(gifts, request.getGifts());

        // 5. 응답 저장
        saveResponses(bundle.getId(), request.getGifts());

        // 6. 번들 상태를 완료로 변경
        bundle.complete();
        bundleRepository.save(bundle);

        return SaveGiftResponsesResponse.of(request.getGifts().size(), gifts.size());
    }

    private Bundle validateAndGetBundle(String link) {
        Bundle bundle = findBundleByLink(link);

        if (bundle.getStatus() == BundleStatus.COMPLETED) {
            throw new BaseException(BaseResponseStatus.ALREADY_ANSWERED);
        }

        return bundle;
    }

    private List<Gift> validateAndGetGifts(Long bundleId, List<SaveGiftResponsesRequest.GiftResponse> giftResponses) {
        Set<Long> requestGiftIds = giftResponses.stream()
                .map(SaveGiftResponsesRequest.GiftResponse::getGiftId)
                .collect(Collectors.toSet());

        List<Gift> gifts = giftRepository.findAllByBundleId(bundleId);

        Set<Long> existingGiftIds = gifts.stream()
                .map(Gift::getId)
                .collect(Collectors.toSet());

        if (!existingGiftIds.containsAll(requestGiftIds)) {
            throw new BaseException(BaseResponseStatus.INVALID_GIFT_ID);
        }

        return gifts;
    }

    private void validateNoExistingResponses(List<Gift> gifts) {
        List<Long> giftIds = gifts.stream()
                .map(Gift::getId)
                .collect(Collectors.toList());

        if (responseRepository.existsByGiftIdIn(giftIds)) {
            throw new BaseException(BaseResponseStatus.ALREADY_ANSWERED);
        }
    }

    private void validateAllGiftsResponded(List<Gift> gifts, List<SaveGiftResponsesRequest.GiftResponse> responses) {
        if (gifts.size() != responses.size()) {
            throw new BaseException(BaseResponseStatus.INCOMPLETE_RESPONSES);
        }
    }

    private void saveResponses(Long bundleId, List<SaveGiftResponsesRequest.GiftResponse> giftResponses) {
        List<Response> responses = giftResponses.stream()
                .map(giftResponse -> {
                    GiftResponseTag giftResponseTagTag = validateAndParseResponseTag(giftResponse.getResponseTag());

                    // Gift 엔티티의 responseTag도 업데이트하도록 수정
                    Gift gift = giftRepository.findById(giftResponse.getGiftId())
                            .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_GIFT_ID));

                    log.debug("Updating giftId: {} with responseTag: {}", gift.getId(), giftResponseTagTag);

                    gift.updateResponse(giftResponseTagTag);
                    giftRepository.save(gift);

                    return Response.builder()
                            .giftId(gift.getId())
                            .bundleId(bundleId)
                            .responseTag(giftResponseTagTag)
                            .build();
                })
                .collect(Collectors.toList());

        responseRepository.saveAll(responses);
    }


    private GiftResponseTag validateAndParseResponseTag(String responseTag) {
        try {
            return GiftResponseTag.valueOf(responseTag);
        } catch (IllegalArgumentException e) {
            throw new BaseException(BaseResponseStatus.INVALID_RESPONSE_TYPE);
        }
    }

    private Bundle findBundleByLink(String link) {
        return bundleRepository.findByLink(link)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_LINK));
    }

    private List<Gift> findGiftsByBundleId(Long bundleId) {
        return giftRepository.findAllByBundleId(bundleId);
    }

    private List<GiftImage> findGiftImages(List<Gift> gifts) {
        return giftImageRepository.findAllByGift_IdIn(
                gifts.stream()
                        .map(Gift::getId)
                        .collect(Collectors.toList())
        );
    }

    private List<Response> findResponses(Long bundleId, List<Gift> gifts) {
        return responseRepository.findAllByBundleIdAndGiftIds(
                bundleId,
                gifts.stream()
                        .map(Gift::getId)
                        .collect(Collectors.toList())
        );
    }

    // PUBLISHED 및 COMPLETED 시에도 정상 처리
    private void validateBundleStatus(Bundle bundle) {
        switch (bundle.getStatus()) {
            case DRAFT -> throw new BaseException(BaseResponseStatus.NOT_DELIVERED_YET);
            case PUBLISHED, COMPLETED -> { /* 정상 처리 */ }
            default -> throw new BaseException(BaseResponseStatus.INVALID_LINK);
        }
    }


    private void updateGiftResponseStatus(List<Gift> gifts, List<Response> responses) {
        List<Long> respondedGiftIds = responses.stream()
                .map(Response::getGiftId)
                .collect(Collectors.toList());

        gifts.forEach(gift -> {
            if (respondedGiftIds.contains(gift.getId())) {
                gift.setResponded(true);
            }
        });
    }
}