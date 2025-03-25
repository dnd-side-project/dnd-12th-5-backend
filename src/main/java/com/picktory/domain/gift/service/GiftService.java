package com.picktory.domain.gift.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.domain.bundle.dto.BundleResultGiftResponse;
import com.picktory.domain.bundle.dto.BundleSummaryResponse;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.gift.dto.DraftGiftsResponse;
import com.picktory.domain.gift.dto.GiftDetailResponse;
import com.picktory.domain.gift.dto.GiftImageRequest;
import com.picktory.domain.gift.dto.GiftUpdateRequest;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GiftService {

    private final GiftRepository giftRepository;
    private final GiftImageRepository giftImageRepository;


    public List<Gift> getGiftsByBundleId(Long bundleId) {
        return giftRepository.findAllByBundleId(bundleId);
    }

    public void updateGifts(Long bundleId, List<GiftUpdateRequest> requests) {
        List<Gift> existingGifts = giftRepository.findAllByBundleId(bundleId);
        Map<Long, Gift> existingGiftMap = existingGifts.stream()
                .collect(Collectors.toMap(Gift::getId, gift -> gift));

        // 기존 이미지 삭제
        deleteImagesByGiftIds(existingGifts.stream().map(Gift::getId).toList());

        // 요청 ID만 추출
        List<Long> requestIds = requests.stream()
                .map(GiftUpdateRequest::getId)
                .filter(Objects::nonNull)
                .toList();

        // 삭제 대상
        List<Gift> toDelete = existingGifts.stream()
                .filter(g -> !requestIds.contains(g.getId()))
                .toList();
        deleteGifts(toDelete);

        List<Gift> remainingGifts = existingGifts.stream()
                .filter(g -> !toDelete.contains(g))
                .toList();

        List<Gift> updated = new ArrayList<>();
        List<Gift> newGifts = new ArrayList<>();
        List<GiftImage> newImages = new ArrayList<>();

        Map<Long, GiftUpdateRequest> requestMap = requests.stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(GiftUpdateRequest::getId, r -> r));

        for (Gift gift : remainingGifts) {
            GiftUpdateRequest req = requestMap.get(gift.getId());
            if (!isGiftUnchanged(gift, req)) {
                gift.updateGift(req);
                updated.add(gift);
            }
            newImages.addAll(createImages(gift, req.getImageUrls()));
        }

        // 새 선물 저장
        for (GiftUpdateRequest req : requests) {
            if (req.getId() == null) {
                Gift newGift = saveGift(Gift.createGift(bundleId, req));
                newGifts.add(newGift);
                newImages.addAll(createImages(newGift, req.getImageUrls()));
            }
        }

        if (!updated.isEmpty()) {
            giftRepository.saveAll(updated);
        }

        if (!newGifts.isEmpty()) {
            giftRepository.saveAll(newGifts);
        }

        if (!newImages.isEmpty()) {
            giftImageRepository.saveAll(newImages);
        }
    }

    public void deleteAllGiftsAndImagesByBundleId(Long bundleId) {
        List<Gift> gifts = giftRepository.findAllByBundleId(bundleId);
        List<Long> giftIds = gifts.stream().map(Gift::getId).toList();

        if (!giftIds.isEmpty()) {
            giftImageRepository.deleteAllByGift_IdIn(giftIds);
            giftRepository.deleteAll(gifts);
        }
    }
    public List<BundleResultGiftResponse> getGiftResultResponsesByBundleId(Long bundleId) {
        List<Gift> gifts = getGiftsByBundleId(bundleId);

        return gifts.stream()
                .map(gift -> {
                    GiftImage primary = findPrimaryOrFirstImage(gift.getId());
                    return BundleResultGiftResponse.from(gift, primary);
                })
                .toList();
    }

    private GiftImage findPrimaryOrFirstImage(Long giftId) {
        return getPrimaryImageByGiftId(giftId)
                .orElseGet(() -> {
                    List<GiftImage> images = getImagesByGiftId(giftId);
                    return images.isEmpty() ? null : images.get(0);
                });
    }


    public List<GiftImage> createGiftImagesWithPrimary(List<? extends GiftImageRequest> giftRequests, List<Gift> savedGifts) {
        List<GiftImage> images = new ArrayList<>();
        for (int i = 0; i < savedGifts.size(); i++) {
            Gift gift = savedGifts.get(i);
            GiftImageRequest giftRequest = giftRequests.get(i);
            List<String> imageUrls = giftRequest.getImageUrls();

            if (imageUrls == null || imageUrls.isEmpty()) {
                throw new BaseException(BaseResponseStatus.GIFT_IMAGE_REQUIRED);
            }

            for (int j = 0; j < imageUrls.size(); j++) {
                boolean isPrimary = (j == 0);
                images.add(GiftImage.createGiftImage(gift, imageUrls.get(j), isPrimary));
            }
        }
        return images;
    }


    public BundleSummaryResponse getGiftSummary(Bundle bundle) {
        List<Gift> gifts = giftRepository.findAllByBundleId(bundle.getId());
        List<GiftImage> images = giftImageRepository.findAllByGift_IdIn(
                gifts.stream().map(Gift::getId).toList()
        );
        return BundleSummaryResponse.fromEntity(bundle, gifts, images);
    }

    public GiftDetailResponse getGiftDetail(Long bundleId, Long giftId) {
        Gift gift = giftRepository.findByIdAndBundleId(giftId, bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.GIFT_NOT_FOUND));

        List<GiftImage> images = giftImageRepository.findAllByGift_Id(giftId);
        return GiftDetailResponse.fromEntity(gift, images);
    }

    public DraftGiftsResponse getDraftGifts(Long bundleId) {
        List<Gift> gifts = giftRepository.findAllByBundleId(bundleId);
        List<GiftImage> images = giftImageRepository.findAllByGift_IdIn(
                gifts.stream().map(Gift::getId).toList()
        );
        return DraftGiftsResponse.from(bundleId, gifts, images);
    }



    public List<GiftImage> getImagesByGiftId(Long giftId) {
        return giftImageRepository.findAllByGift_Id(giftId);
    }

    public void deleteGifts(List<Gift> gifts) {
        giftRepository.deleteAll(gifts);
    }

    public void deleteImagesByGiftIds(List<Long> giftIds) {
        giftImageRepository.deleteAllByGift_IdIn(giftIds);
    }

    public Gift getGiftByIdAndBundleId(Long giftId, Long bundleId) {
        return giftRepository.findByIdAndBundleId(giftId, bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.GIFT_NOT_FOUND));
    }

    public List<GiftImage> getImagesByGiftIds(List<Long> giftIds) {
        return giftImageRepository.findAllByGift_IdIn(giftIds);
    }

    public void saveGiftImages(List<GiftImage> images) {
        giftImageRepository.saveAll(images);
    }


    public List<Gift> saveGifts(List<Gift> gifts) {
        return giftRepository.saveAll(gifts);
    }


    public Gift saveGift(Gift gift) {
        return giftRepository.save(gift);
    }

    public Optional<GiftImage> getPrimaryImageByGiftId(Long giftId) {
        return giftImageRepository.findByGift_IdAndIsPrimaryTrue(giftId);
    }

    private boolean isGiftUnchanged(Gift gift, GiftUpdateRequest req) {
        return gift.getName().equals(req.getName()) &&
                Objects.equals(gift.getMessage(), req.getMessage()) &&
                Objects.equals(gift.getPurchaseUrl(), req.getPurchaseUrl());
    }

    private List<GiftImage> createImages(Gift gift, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            throw new BaseException(BaseResponseStatus.GIFT_IMAGE_REQUIRED);
        }

        List<GiftImage> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            images.add(GiftImage.createGiftImage(gift, urls.get(i), i == 0));
        }
        return images;
    }

    private void logGifts(String title, List<Gift> gifts) {
        log.debug("=== {} ===", title);
        for (Gift gift : gifts) {
            log.debug("[id: {}] name: {}, message: {}, purchaseUrl: {}",
                    gift.getId(), gift.getName(), gift.getMessage(), gift.getPurchaseUrl());
        }
    }

}
