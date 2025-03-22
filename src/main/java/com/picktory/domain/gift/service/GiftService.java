package com.picktory.domain.gift.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.domain.bundle.dto.BundleResultGiftResponse;
import com.picktory.domain.gift.dto.GiftImageRequest;
import com.picktory.domain.gift.dto.GiftUpdateRequest;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GiftService {

    private final GiftRepository giftRepository;
    private final GiftImageRepository giftImageRepository;

    public List<Gift> getGiftsByBundleId(Long bundleId) {
        return giftRepository.findByBundleId(bundleId);
    }

    public void updateGifts(Long bundleId, List<GiftUpdateRequest> requests) {
        List<Gift> existingGifts = giftRepository.findByBundleId(bundleId);
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

        // 기존 수정
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
        List<Gift> gifts = giftRepository.findByBundleId(bundleId);
        List<Long> giftIds = gifts.stream().map(Gift::getId).toList();

        if (!giftIds.isEmpty()) {
            giftImageRepository.deleteByGiftIds(giftIds);
            giftRepository.deleteAll(gifts);
        }
    }

    public List<BundleResultGiftResponse> getGiftResultResponsesByBundleId(Long bundleId) {
        List<Gift> gifts = getGiftsByBundleId(bundleId);

        return gifts.stream()
                .map(gift -> {
                    GiftImage primary = getPrimaryImageByGiftId(gift.getId())
                            .orElseGet(() -> getImagesByGiftId(gift.getId()).stream().findFirst().orElse(null));
                    return BundleResultGiftResponse.from(gift, primary);
                })
                .toList();
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


    public List<GiftImage> getImagesByGiftId(Long giftId) {
        return giftImageRepository.findByGiftId(giftId);
    }

    public void deleteGifts(List<Gift> gifts) {
        giftRepository.deleteAll(gifts);
    }

    public void deleteImagesByGiftIds(List<Long> giftIds) {
        giftImageRepository.deleteByGiftIds(giftIds);
    }

    public Gift getGiftByIdAndBundleId(Long giftId, Long bundleId) {
        return giftRepository.findByIdAndBundleId(giftId, bundleId)
                .orElseThrow(() -> new RuntimeException("선물을 찾을 수 없습니다."));
    }

    public List<GiftImage> getImagesByGiftIds(List<Long> giftIds) {
        return giftImageRepository.findByGiftIdIn(giftIds);
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
        return giftImageRepository.findPrimaryImageByGiftId(giftId);
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

}
