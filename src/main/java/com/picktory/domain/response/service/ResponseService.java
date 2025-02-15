package com.picktory.domain.response.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import com.picktory.domain.response.dto.ResponseBundleDto;
import com.picktory.domain.response.entity.Response;
import com.picktory.domain.response.entity.ResponseTag;
import com.picktory.domain.response.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResponseService {
    private final BundleRepository bundleRepository;
    private final GiftRepository giftRepository;
    private final GiftImageRepository giftImageRepository;
    private final ResponseRepository responseRepository;

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
    public void createResponse(Long giftId, Long bundleId, ResponseTag responseTag, String message) {
        validateResponseNotExists(giftId);
        Response response = buildResponse(giftId, bundleId, responseTag, message);
        responseRepository.save(response);
    }

    private Bundle findBundleByLink(String link) {
        return bundleRepository.findByLink(link)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.INVALID_LINK));
    }

    private List<Gift> findGiftsByBundleId(Long bundleId) {
        return giftRepository.findByBundleId(bundleId);
    }

    private List<GiftImage> findGiftImages(List<Gift> gifts) {
        return giftImageRepository.findByGiftIdIn(
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

    private void validateBundleStatus(Bundle bundle) {
        switch (bundle.getStatus()) {
            case DRAFT -> throw new BaseException(BaseResponseStatus.NOT_DELIVERED_YET);
            case COMPLETED -> throw new BaseException(BaseResponseStatus.INVALID_BUNDLE_STATUS);
            case PUBLISHED -> { /* 정상 처리 */ }
            default -> throw new BaseException(BaseResponseStatus.INVALID_LINK);
        }
    }

    private void validateResponseNotExists(Long giftId) {
        if (responseRepository.existsByGiftId(giftId)) {
            throw new BaseException(BaseResponseStatus.INVALID_BUNDLE_STATUS);
        }
    }

    private Response buildResponse(Long giftId, Long bundleId, ResponseTag responseTag, String message) {
        return Response.builder()
                .giftId(giftId)
                .bundleId(bundleId)
                .responseTag(responseTag)
                .message(message)
                .build();
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