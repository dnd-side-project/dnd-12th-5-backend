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
import com.picktory.domain.gift.service.GiftService;
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
    private final GiftService giftService;

    /**
     * 보따리 생성
     */
    public BundleResponse createBundle(BundleRequest request) {
        User currentUser = authenticationService.getAuthenticatedUser();

        // 프론트 테스트 위해 하루 보따리 생성 개수 테스트 비활성화
//        // 하루 보따리 생성 개수 제한 검사
//        long todayBundleCount = bundleRepository.countByUserIdAndCreatedAtAfter(
//                currentUser.getId(), LocalDateTime.now().toLocalDate().atStartOfDay()
//        );
//        if (todayBundleCount >= 10) {
//            throw new BaseException(BaseResponseStatus.BUNDLE_DAILY_LIMIT_EXCEEDED);
//        }

        // 보따리 유효성 검증
        validateBundleRequest(request);

        // 1. 보따리 저장
        Bundle bundle = bundleRepository.save(request.toEntity(currentUser));

        // 2. 선물 저장
        List<Gift> gifts = request.getGifts().stream()
                .map(giftRequest -> Gift.createGift(bundle.getId(), giftRequest))
                .toList();
        List<Gift> savedGifts = giftService.saveGifts(gifts);

        // 3. 선물 이미지 저장
        List<GiftImage> newImages = giftService.createGiftImagesWithPrimary(request.getGifts(), savedGifts);
        giftService.saveGiftImages(newImages);

        return BundleResponse.fromEntity(bundle, savedGifts, newImages);
    }

    /**
     * 보따리 업데이트
     */
    @Transactional
    public BundleResponse updateBundle(Long bundleId, BundleUpdateRequest request) {
        log.info("보따리 업데이트 요청: bundleId = {}", bundleId);

        User currentUser = authenticationService.getAuthenticatedUser();
        Bundle bundle = validateAndGetBundle(bundleId, currentUser);

        if (bundle.getStatus() != BundleStatus.DRAFT) {
            throw new BaseException(BaseResponseStatus.INVALID_BUNDLE_STATUS_FOR_DRAFT);
        }

        // 1. 기존 선물 업데이트 처리 (삭제/수정/추가 + 이미지 포함)
        giftService.updateGifts(bundle.getId(), request.getGifts());

        // 2. 최종 저장된 선물, 이미지 조회
        List<Gift> savedGifts = giftService.getGiftsByBundleId(bundleId);
        List<GiftImage> savedImages = giftService.getImagesByGiftIds(
                savedGifts.stream().map(Gift::getId).toList()
        );
        return BundleResponse.fromEntity(bundle, savedGifts, savedImages);
    }

    /**
     * 업데이트 목록에 포함되지 않은 기존 선물 삭제
     */
    private void deleteGifts(List<Gift> giftsToDelete) {
        if (!giftsToDelete.isEmpty()) {
            List<Long> giftIdsToDelete = giftsToDelete.stream().map(Gift::getId).toList();
            giftRepository.deleteAll(giftsToDelete);
            log.info("삭제된 선물 수: {}", giftIdsToDelete.size());
        }
    }

    /**
     * 기존 선물 이미지 모두 삭제
     */
    private void deleteAllGiftImages(List<Gift> existingGifts) {
        List<Long> existingGiftIds = existingGifts.stream()
                .map(Gift::getId)
                .toList();
        if (!existingGiftIds.isEmpty()) {
            giftImageRepository.deleteByGiftIds(existingGiftIds);
            log.info("기존 선물 이미지 삭제 완료: {}", existingGiftIds.size());
        }
    }

    /**
     * 선물 수정 여부 판단 (imageurls 제외)
     */
    private boolean isGiftUnchanged(Gift existingGift, GiftUpdateRequest newGift) {
        return existingGift.getName().equals(newGift.getName()) &&
                Objects.equals(existingGift.getMessage(), newGift.getMessage()) &&
                Objects.equals(existingGift.getPurchaseUrl(), newGift.getPurchaseUrl());
    }

    /**
     * 업데이트 목록에 포함된 기존 선물의 이미지 저장
     */
    private List<GiftImage> processGiftImages(List<GiftUpdateRequest> giftRequests, List<Gift> remainingGifts) {
        List<GiftImage> newImages = new ArrayList<>();

        // 요청된 선물 리스트를 Map으로 변환
        Map<Long, GiftUpdateRequest> giftRequestMap = giftRequests.stream()
                .filter(req -> req.getId() != null) // 기존 선물만 매핑
                .collect(Collectors.toMap(GiftUpdateRequest::getId, req -> req));

        for (Gift gift : remainingGifts) {
            GiftUpdateRequest giftRequest = giftRequestMap.get(gift.getId());

            if (giftRequest == null) {
                log.error("기존 선물 ID {}에 대한 요청 데이터가 없음", gift.getId());
                throw new BaseException(BaseResponseStatus.INVALID_GIFT_UPDATE);
            }

            List<String> newImageUrls = giftRequest.getImageUrls();
            if (newImageUrls == null || newImageUrls.isEmpty()) {
                throw new BaseException(BaseResponseStatus.GIFT_IMAGE_REQUIRED);
            }

            // 대표 이미지 저장
            newImages.add(GiftImage.createGiftImage(gift, newImageUrls.get(0), true));

            // 나머지 이미지 저장
            for (int i = 1; i < newImageUrls.size(); i++) {
                newImages.add(GiftImage.createGiftImage(gift, newImageUrls.get(i), false));
            }
        }
        return newImages;
    }


    /**
     * 사용자의 보따리 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BundleListResponse> getMyBundles(User user) {
        List<Bundle> bundles = bundleRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        return BundleListResponse.listFrom(bundles);
    }

    /**
     * 사용자의 최신 8개 보따리 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BundleMainListResponse> getUserMainBundles() {
        User currentUser = authenticationService.getAuthenticatedUser();
        List<Bundle> bundles = bundleRepository.findTop8ByUser_IdOrderByUpdatedAtDesc(currentUser.getId());
        return BundleMainListResponse.listFrom(bundles);
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

            if (imageUrls == null || imageUrls.isEmpty()) {
                throw new BaseException(BaseResponseStatus.GIFT_IMAGE_REQUIRED);
            }

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
        User currentUser = authenticationService.getAuthenticatedUser();
        Bundle bundle = validateAndGetBundle(bundleId, currentUser);

        log.info("보따리 삭제 시작 - bundleId: {}, userId: {}", bundleId, currentUser.getId());

        giftService.deleteAllGiftsAndImagesByBundleId(bundleId);

        bundleRepository.delete(bundle);

        log.info("보따리 삭제 완료 - bundleId: {}", bundleId);
    }


    /**
     * 보따리 결과 조회
     */
    public BundleResultResponse getBundleResult(Long bundleId) {
        User currentUser = authenticationService.getAuthenticatedUser();

        Bundle bundle = bundleRepository.findByIdAndStatus(bundleId, BundleStatus.COMPLETED)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));

        if (!bundle.getUser().getId().equals(currentUser.getId())) {
            throw new BaseException(BaseResponseStatus.FORBIDDEN);
        }

        List<BundleResultGiftResponse> giftResponses = giftService.getGiftResultResponsesByBundleId(bundleId);

        return new BundleResultResponse(bundle.getId(), giftResponses);
    }

    /**
     * 보따리 조회 API (간이 조회)
     */
    @Transactional
    public BundleSummaryResponse getBundle(Long bundleId) {
        User currentUser = authenticationService.getAuthenticatedUser();

        // 보따리 조회 및 사용자 검증
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));

        if (!bundle.getUser().getId().equals(currentUser.getId())) {
            throw new BaseException(BaseResponseStatus.FORBIDDEN);
        }

        // 보따리에 포함된 선물 목록 조회
        List<Gift> gifts = giftRepository.findByBundleId(bundleId);

        // 각 선물의 대표 이미지 가져오기
        List<GiftImage> images = giftImageRepository.findByGiftIdIn(gifts.stream().map(Gift::getId).toList());

        // 최초 조회 시 isRead 업데이트
        if (bundle.getStatus() == BundleStatus.COMPLETED && !bundle.getIsRead()) {
            bundle.markAsRead();
        }

        return BundleSummaryResponse.fromEntity(bundle, gifts, images);
    }


    private Bundle validateAndGetBundle(Long bundleId, User currentUser) {
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));
        if (!bundle.getUser().getId().equals(currentUser.getId())) {
            throw new BaseException(BaseResponseStatus.FORBIDDEN);
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

        return DraftGiftsResponse.from(bundle.getId(), gifts, images); // (수정) bundleId도 전달
    }
}