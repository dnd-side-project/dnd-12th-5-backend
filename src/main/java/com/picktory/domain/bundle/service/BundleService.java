package com.picktory.domain.bundle.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.config.auth.AuthenticationService;
import com.picktory.domain.bundle.dto.*;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.gift.dto.*;
import com.picktory.domain.gift.entity.Gift;
import com.picktory.domain.gift.entity.GiftImage;
import com.picktory.domain.gift.repository.GiftImageRepository;
import com.picktory.domain.gift.repository.GiftRepository;
import com.picktory.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class BundleService {

    private final BundleRepository bundleRepository;
    private final GiftRepository giftRepository;
    private final GiftImageRepository giftImageRepository;
    private final AuthenticationService authenticationService;

    /**
     * 보따리 최초 생성
     */
    public BundleResponse createBundle(BundleRequest request) {
        User currentUser = authenticationService.getAuthenticatedUser();

        // 하루 보따리 생성 개수 제한 검사
        long todayBundleCount = bundleRepository.countByUserIdAndCreatedAtAfter(
                currentUser.getId(), LocalDateTime.now().toLocalDate().atStartOfDay()
        );
        if (todayBundleCount >= 10) {
            throw new BaseException(BaseResponseStatus.BUNDLE_DAILY_LIMIT_EXCEEDED);
        }

        // 보따리 유효성 검증
        validateBundleRequest(request);

        // 보따리 저장
        Bundle bundle = bundleRepository.save(Bundle.builder()
                .userId(currentUser.getId())
                .name(request.getName())
                .designType(request.getDesignType())
                .deliveryCharacterType(null)
                .status(BundleStatus.DRAFT)
                .isRead(false)
                .build());

        // 선물 저장
        List<Gift> gifts = request.getGifts().stream()
                .map(giftRequest -> Gift.createGift(bundle.getId(), giftRequest))
                .toList();
        List<Gift> savedGifts = giftRepository.saveAll(gifts);

        // 선물 이미지 저장 및 대표 이미지 설정
        List<GiftImage> newImages = setPrimaryImage(request.getGifts(), savedGifts);
        giftImageRepository.saveAll(newImages);

        return BundleResponse.fromEntity(bundle, savedGifts, newImages);
    }

    /**
     * 보따리 업데이트 (선물 수정, 삭제, 추가)
     */
    public BundleResponse updateBundle(Long bundleId, BundleUpdateRequest request) {
        User currentUser = authenticationService.getAuthenticatedUser();

        // 보따리 조회 및 검증
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));
        if (!bundle.getUserId().equals(currentUser.getId())) {
            throw new BaseException(BaseResponseStatus.BUNDLE_ACCESS_DENIED);
        }

        validateBundleUpdateRequest(request);

        // 기존 선물 조회 및 매핑
        List<Gift> existingGifts = giftRepository.findByBundleId(bundleId);
        Map<Long, Gift> existingGiftMap = existingGifts.stream()
                .collect(Collectors.toMap(Gift::getId, gift -> gift));

        List<Gift> updatedGifts = new ArrayList<>();
        List<Gift> newGifts = new ArrayList<>();
        List<Long> receivedGiftIds = new ArrayList<>();

        // 선물 업데이트 및 추가
        for (GiftUpdateRequest giftUpdateRequest : request.getGifts()) {
            if (giftUpdateRequest.getId() != null && existingGiftMap.containsKey(giftUpdateRequest.getId())) {
                // 기존 선물 업데이트
                Gift existingGift = existingGiftMap.get(giftUpdateRequest.getId());
                existingGift.updateGift(giftUpdateRequest);
                updatedGifts.add(existingGift);
                receivedGiftIds.add(existingGift.getId());
            } else {
                // 새로운 선물 추가
                newGifts.add(Gift.createGift(bundle.getId(), giftUpdateRequest));
            }
        }

        // 삭제할 선물 찾기 (요청에 없는 기존 선물)
        List<Gift> giftsToDelete = existingGifts.stream()
                .filter(gift -> !receivedGiftIds.contains(gift.getId()))
                .toList();

        // 삭제해야 할 선물 ID 리스트
        List<Long> giftIdsToDelete = giftsToDelete.stream().map(Gift::getId).toList();
        if (!giftIdsToDelete.isEmpty()) {
            giftImageRepository.deleteByGiftIds(giftIdsToDelete);
            giftRepository.deleteByIds(giftIdsToDelete);
        }

        // 저장
        List<Gift> savedGifts = giftRepository.saveAll(updatedGifts);
        List<Gift> newlySavedGifts = giftRepository.saveAll(newGifts);
        savedGifts.addAll(newlySavedGifts);

        // 선물 이미지 저장 및 대표 이미지 설정
        List<GiftImage> newImages = setPrimaryImage(request.getGifts(), savedGifts);
        giftImageRepository.saveAll(newImages);

        return BundleResponse.fromEntity(bundle, savedGifts, newImages);
    }

    /**
     * 보따리 유효성 검증
     */
    private void validateBundleRequest(BundleRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BaseException(BaseResponseStatus.BUNDLE_NAME_REQUIRED);
        }
        if (request.getDesignType() == null) {
            throw new BaseException(BaseResponseStatus.BUNDLE_DESIGN_REQUIRED);
        }
        if (request.getGifts() == null || request.getGifts().size() < 2) {
            throw new BaseException(BaseResponseStatus.BUNDLE_MINIMUM_GIFTS_REQUIRED);
        }
    }

    private void validateBundleUpdateRequest(BundleUpdateRequest request) {
        if (request.getGifts() == null || request.getGifts().size() < 2) {
            throw new BaseException(BaseResponseStatus.BUNDLE_MINIMUM_GIFTS_REQUIRED);
        }
    }

    /**
     * 대표 이미지 설정 로직 (각 선물당 첫 번째 이미지를 대표 이미지로 설정)
     */
    private List<GiftImage> setPrimaryImage(List<? extends GiftImageRequest> giftRequests, List<Gift> savedGifts) {
        List<GiftImage> newImages = new ArrayList<>();

        // 기존 선물 ID와 이미지 매핑 (ID가 없는 경우 -1 * (i+1) 사용하여 중복 방지)
        Map<Long, List<String>> giftImagesMap = IntStream.range(0, giftRequests.size())
                .boxed()
                .collect(Collectors.toMap(
                        i -> Optional.ofNullable(giftRequests.get(i).getId()).orElse(-1L * (i + 1)),
                        i -> Optional.ofNullable(giftRequests.get(i).getImageUrls()).orElseGet(ArrayList::new),
                        (existing, replacement) -> existing // 중복 키 발생 시 기존 값 유지
                ));

        for (Gift gift : savedGifts) {
            List<String> imageUrls = giftImagesMap.getOrDefault(gift.getId(), new ArrayList<>());

            for (int i = 0; i < imageUrls.size(); i++) {
                newImages.add(GiftImage.createGiftImage(gift.getId(), imageUrls.get(i), i == 0));
            }
        }
        return newImages;
    }

}
