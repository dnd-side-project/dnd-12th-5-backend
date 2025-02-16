package com.picktory.domain.bundle.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.config.auth.AuthenticationService;

import com.picktory.domain.bundle.dto.*;
import com.picktory.domain.bundle.dto.BundleDeliveryRequest;
import com.picktory.domain.bundle.dto.BundleRequest;
import com.picktory.domain.bundle.dto.BundleResponse;

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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;


@Slf4j
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
    @Transactional
    public BundleResponse updateBundle(Long bundleId, BundleUpdateRequest request) {
        User currentUser = authenticationService.getAuthenticatedUser();
        Bundle bundle = validateAndGetBundle(bundleId, currentUser);

        // 기존 선물 처리
        List<Gift> existingGifts = giftRepository.findByBundleId(bundleId);
        Map<Long, Gift> existingGiftMap = createGiftMap(existingGifts);
        logExistingGifts(existingGifts);

        // 선물 업데이트 처리
        List<Long> receivedGiftIds = getReceivedGiftIds(request);
        List<Gift> giftsToDelete = getGiftsToDelete(existingGifts, receivedGiftIds);
        validateFinalGiftCount(request, receivedGiftIds);

        // 삭제할 선물 및 이미지 삭제 처리
        deleteGiftsAndImages(giftsToDelete);

        // 수정 및 추가할 선물 처리
        List<Gift> updatedGifts = new ArrayList<>();
        List<Gift> newGifts = new ArrayList<>();

        for (GiftUpdateRequest giftUpdateRequest : request.getGifts()) {
            if (giftUpdateRequest.getId() != null && existingGiftMap.containsKey(giftUpdateRequest.getId())) {
                // 기존 선물 업데이트
                Gift existingGift = Gift.updateGift(existingGiftMap.get(giftUpdateRequest.getId()), giftUpdateRequest);
                updatedGifts.add(existingGift);
            } else {
                // 새 선물 추가
                newGifts.add(Gift.createGift(bundle.getId(), giftUpdateRequest));
            }
        }

        // 선물 저장
        List<Gift> savedGifts = giftRepository.saveAll(updatedGifts);
        savedGifts.addAll(giftRepository.saveAll(newGifts));
        logSavedGifts(savedGifts);

        // 선물 이미지 저장 및 대표 이미지 설정
        List<GiftImage> newImages = setPrimaryImage(request.getGifts(), savedGifts);
        giftImageRepository.saveAll(newImages);

        return BundleResponse.fromEntity(bundle, savedGifts, newImages);
    }

    /**
     * 사용자의 보따리 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BundleListResponse> getUserBundles() {
        User currentUser = authenticationService.getAuthenticatedUser();
        List<Bundle> bundles = bundleRepository.findByUserIdOrderByUpdatedAtDesc(currentUser.getId());
        return BundleListResponse.fromEntityList(bundles);
    }

    /**
     * 사용자의 최신 8개 보따리 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BundleMainListResponse> getUserMainBundles() {
        User currentUser = authenticationService.getAuthenticatedUser();
        List<Bundle> bundles = bundleRepository.findTop8ByUserIdOrderByUpdatedAtDesc(currentUser.getId());
        return BundleMainListResponse.fromEntityList(bundles);
    }

    /**
     * 배달부 캐릭터 설정
     */
    public BundleResponse updateDeliveryCharacter(Long bundleId, BundleDeliveryRequest request) {
        User currentUser = authenticationService.getAuthenticatedUser();
        Bundle bundle = validateAndGetBundle(bundleId, currentUser);

        String link = generateDeliveryLink();
        bundle.updateDeliveryCharacter(request.getDeliveryCharacterType(), link);
        Bundle savedBundle = bundleRepository.save(bundle);

        return BundleResponse.fromEntity(savedBundle, null, null);
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

    /**
     * 대표 이미지 설정 로직 (각 선물당 첫 번째 이미지를 대표 이미지로 설정)
     */
    private List<GiftImage> setPrimaryImage(List<? extends GiftImageRequest> giftRequests, List<Gift> savedGifts) {
        List<GiftImage> newImages = new ArrayList<>();

        // savedGifts와 giftRequests의 순서가 동일하다고 가정
        for (int i = 0; i < savedGifts.size(); i++) {
            Gift gift = savedGifts.get(i);
            GiftImageRequest giftRequest = giftRequests.get(i);
            List<String> imageUrls = giftRequest.getImageUrls();

            // 각 giftRequest의 첫 번째 이미지가 대표 이미지(isPrimary=true)
            for (int j = 0; j < imageUrls.size(); j++) {
                boolean isPrimary = (j == 0);
                newImages.add(GiftImage.createGiftImage(gift, imageUrls.get(j), isPrimary));
            }
        }
        return newImages;
    }

    /**
     * 보따리 삭제
     */
    @Transactional
    public void deleteBundle(Long bundleId) {
        Long userId = authenticationService.getAuthenticatedUser().getId();

        // 보따리 조회 (사용자가 소유한 보따리인지 확인)
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));

        if (!bundle.getUserId().equals(userId)) {
            throw new BaseException(BaseResponseStatus.FORBIDDEN);
        }

        log.info("보따리 삭제 시작 - bundleId: {}, userId: {}", bundleId, userId);

        // 보따리에 속한 선물들 조회
        List<Gift> gifts = giftRepository.findByBundleId(bundleId);
        for (Gift gift : gifts) {
            // 선물에 속한 이미지 삭제
            List<GiftImage> giftImages = giftImageRepository.findByGiftId(gift.getId());

            // 추후 S3 삭제 로직 추가 가능
            // for (GiftImage image : giftImages) {
            //     s3Service.deleteImageFromS3(image.getImageUrl()); // S3에서 삭제
            // }

            // DB에서 이미지 삭제
            giftImageRepository.deleteAll(giftImages);
        }

        // 선물 삭제
        giftRepository.deleteAll(gifts);

        // 보따리 삭제
        bundleRepository.delete(bundle);

        log.info("보따리 삭제 완료 - bundleId: {}", bundleId);
    }

     /**
     * 보따리 결과 조회
     */
    public BundleResultResponse getBundleResult(Long bundleId) {
        User currentUser = authenticationService.getAuthenticatedUser();

        // 보따리 조회 & COMPLETED 상태 검증
        Bundle bundle = bundleRepository.findByIdAndStatus(bundleId, BundleStatus.COMPLETED)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));

        // 본인 보따리인지 확인
        if (!bundle.getUserId().equals(currentUser.getId())) {
            throw new BaseException(BaseResponseStatus.BUNDLE_ACCESS_DENIED);
        }

        // 보따리에 포함된 선물 목록을 직접 조회
        List<Gift> gifts = giftRepository.findByBundleId(bundleId);

        // 각 선물에 대한 대표 이미지 조회 (대표 이미지가 없으면 첫 번째 이미지 반환)
        List<BundleResultGiftResponse> giftResponses = gifts.stream()
                .map(gift -> {
                    GiftImage primaryImage = giftImageRepository.findPrimaryImageByGiftId(gift.getId())
                            .orElseGet(() -> giftImageRepository.findByGiftId(gift.getId()).stream().findFirst().orElse(null));

                    return BundleResultGiftResponse.from(gift, primaryImage);
                })
                .toList();

        return new BundleResultResponse(bundle.getId(), giftResponses);
    }

    private Bundle validateAndGetBundle(Long bundleId, User currentUser) {
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));
        if (!bundle.getUserId().equals(currentUser.getId())) {
            throw new BaseException(BaseResponseStatus.BUNDLE_ACCESS_DENIED);
        }
        return bundle;
    }

    private Map<Long, Gift> createGiftMap(List<Gift> gifts) {
        return gifts.stream().collect(Collectors.toMap(Gift::getId, gift -> gift));
    }

    private void logExistingGifts(List<Gift> gifts) {
        log.debug("=== 기존 선물 목록 ===");
        gifts.forEach(gift -> logGiftDetails("기존 선물", gift));
    }

    private void logSavedGifts(List<Gift> gifts) {
        log.debug("=== 최종 저장된 선물 목록 ===");
        gifts.forEach(gift -> logGiftDetails("최종 저장 선물", gift));
    }

    private List<Long> getReceivedGiftIds(BundleUpdateRequest request) {
        return request.getGifts().stream()
                .map(GiftUpdateRequest::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Gift> getGiftsToDelete(List<Gift> existingGifts, List<Long> receivedGiftIds) {
        return existingGifts.stream()
                .filter(gift -> !receivedGiftIds.contains(gift.getId()))
                .toList();
    }

    private void validateFinalGiftCount(BundleUpdateRequest request, List<Long> receivedGiftIds) {
        long finalGiftCount = receivedGiftIds.size() + (request.getGifts().size() - receivedGiftIds.size());
        if (finalGiftCount < 2) {
            throw new BaseException(BaseResponseStatus.BUNDLE_MINIMUM_GIFTS_REQUIRED);
        }
    }

    private void deleteGiftsAndImages(List<Gift> giftsToDelete) {
        if (!giftsToDelete.isEmpty()) {
            List<Long> giftIdsToDelete = giftsToDelete.stream().map(Gift::getId).toList();
            giftImageRepository.deleteByGiftIds(giftIdsToDelete);
            giftRepository.deleteAll(giftsToDelete);
        }
    }

    private String generateDeliveryLink() {
        return UUID.randomUUID().toString();
    }

    private void logGiftDetails(String prefix, Gift gift) {
        log.debug("{} - [id: {}] name: {}, message: {}, purchaseUrl: {}",
                prefix, gift.getId(), gift.getName(), gift.getMessage(), gift.getPurchaseUrl());
    }
    /**
     * 보따리 개별 선물 조회
     */
    @Transactional(readOnly = true)
    public GiftDetailResponse getGift(Long bundleId, Long giftId) {
        User currentUser = authenticationService.getAuthenticatedUser();

        // 보따리 존재 및 권한 확인
        Bundle bundle = validateAndGetBundle(bundleId, currentUser);

        // 선물 조회
        Gift gift = giftRepository.findByIdAndBundleId(giftId, bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.GIFT_NOT_FOUND));

        // 이미지 조회
        List<GiftImage> images = giftImageRepository.findByGiftId(giftId);

        return GiftDetailResponse.fromEntity(gift, images);
    }


    /**
     * 임시 저장된 보따리의 선물 목록 조회
     */
    @Transactional(readOnly = true)
    public DraftGiftsResponse getDraftGifts(Long bundleId) {
        User currentUser = authenticationService.getAuthenticatedUser();

        // 보따리 존재 및 권한 확인
        Bundle bundle = validateAndGetBundle(bundleId, currentUser);

        // DRAFT 상태 확인
        if (bundle.getStatus() != BundleStatus.DRAFT) {
            throw new BaseException(BaseResponseStatus.INVALID_BUNDLE_STATUS);
        }

        // 선물 목록 조회
        List<Gift> gifts = giftRepository.findByBundleId(bundleId);

        // 선물 이미지 조회
        List<GiftImage> images = giftImageRepository.findByGiftIds(
                gifts.stream().map(Gift::getId).collect(Collectors.toList())
        );

        return DraftGiftsResponse.from(gifts, images);
    }
}
