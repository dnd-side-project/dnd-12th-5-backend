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
     * 보따리 생성
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

        // 1. 보따리 저장
        Bundle bundle = bundleRepository.save(Bundle.builder()
                .userId(currentUser.getId())
                .name(request.getName())
                .designType(request.getDesignType())
                .status(BundleStatus.DRAFT)
                .isRead(false)
                .build());

        // 2. 선물 저장 (Gift 먼저 저장, ID 생성)
        List<Gift> gifts = request.getGifts().stream()
                .map(giftRequest -> Gift.createGift(bundle.getId(), giftRequest))
                .toList();
        List<Gift> savedGifts = giftRepository.saveAll(gifts); // Gift 먼저 저장

        // 3. 선물 이미지 저장 (Gift ID가 존재하는 상태에서 저장)
        List<GiftImage> newImages = setPrimaryImage(request.getGifts(), savedGifts);
        giftImageRepository.saveAll(newImages);

        return BundleResponse.fromEntity(bundle, savedGifts, newImages);
    }

    /**
     * 보따리 업데이트
     */
    @Transactional
    public BundleResponse updateBundle(Long bundleId, BundleUpdateRequest request) {
        log.info("보따리 업데이트 요청: bundleId = {}", bundleId);

        // 1. 현재 사용자 정보 조회
        User currentUser = authenticationService.getAuthenticatedUser();

        // 2. 보따리 유효성 검사 및 조회
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));
        if (!bundle.getUserId().equals(currentUser.getId())) {
            throw new BaseException(BaseResponseStatus.FORBIDDEN);
        }
        if (bundle.getStatus() != BundleStatus.DRAFT) {
            throw new BaseException(BaseResponseStatus.INVALID_BUNDLE_STATUS_FOR_DRAFT);
        }

        // 3. 기존 선물 조회
        List<Gift> existingGifts = giftRepository.findByBundleId(bundleId);
        Map<Long, Gift> existingGiftMap = existingGifts.stream()
                .collect(Collectors.toMap(Gift::getId, gift -> gift));

        // 4. 기존 선물 이미지 삭제 (대표 이미지 처리 문제 방지)
        deleteAllGiftImages(existingGifts);
        log.info("기존 선물 이미지 삭제 완료");

        // 5. 기존 선물 중 요청에서 빠진 선물 삭제
        List<Long> receivedGiftIds = request.getGifts().stream()
                .map(GiftUpdateRequest::getId)
                .filter(Objects::nonNull)
                .toList();
        List<Gift> giftsToDelete = existingGifts.stream()
                .filter(gift -> !receivedGiftIds.contains(gift.getId()))
                .toList();
        deleteGifts(giftsToDelete);
        log.info("삭제된 선물 수: {}", giftsToDelete.size());

        // 6. 삭제되지 않은 기존 선물 리스트 생성
        List<Gift> remainingGifts = existingGifts.stream()
                .filter(gift -> !giftsToDelete.contains(gift))
                .toList();

        // 7. 기존 선물 이미지 업데이트 (삭제되지 않은 기존 선물 대상)
        List<GiftImage> newImages = processGiftImages(request.getGifts(), remainingGifts);
        giftImageRepository.saveAll(newImages);
        log.info("기존 선물 이미지 재생성 완료, 저장된 이미지 수: {}", newImages.size());

        // 8. 요청된 선물들을 순회하며 수정 또는 추가 처리
        List<Gift> updatedGifts = new ArrayList<>();

        for (GiftUpdateRequest giftRequest : request.getGifts()) {
            if (giftRequest.getId() != null && existingGiftMap.containsKey(giftRequest.getId())) {
                // 기존 선물 업데이트 확인 후 수정
                Gift existingGift = existingGiftMap.get(giftRequest.getId());
                if (!isGiftUnchanged(existingGift, giftRequest)) {
                    existingGift.updateGift(giftRequest);
                    updatedGifts.add(existingGift);
                }
            } else {
                // 새로운 선물 즉시 DB 저장
                giftRepository.save(Gift.createGift(bundle.getId(), giftRequest));
            }
        }

        // 9. 기존 선물 변경 사항 저장
        if (!updatedGifts.isEmpty()) {
            giftRepository.saveAll(updatedGifts);
        }

        log.info("수정된 선물 수: {}", updatedGifts.size());

        // 10. 최종 저장된 선물, 이미지 데이터 조회
        List<Gift> savedGifts = giftRepository.findByBundleId(bundleId);
        List<GiftImage> savedImages = giftImageRepository.findByGiftIdIn(
                savedGifts.stream().map(Gift::getId).toList());

        // 11. 최종 응답 반환 (DB에서 저장된 데이터 기반)
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

    /**
     * 보따리 조회 API (간이 조회)
     */
    @Transactional
    public BundleSummaryResponse getBundle(Long bundleId) {
        User currentUser = authenticationService.getAuthenticatedUser();

        // 보따리 조회 및 사용자 검증
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));

        if (!bundle.getUserId().equals(currentUser.getId())) {
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

    // 보따리 업데이트: 기존 선물 및 이미지 삭제
//    private void deleteGiftsAndImages(List<Gift> giftsToDelete) {
//        if (!giftsToDelete.isEmpty()) {
//            List<Long> giftIdsToDelete = giftsToDelete.stream().map(Gift::getId).toList();
//            giftImageRepository.deleteByGiftIds(giftIdsToDelete);
//            giftRepository.deleteAll(giftsToDelete);
//        }
//    }
    // 보따리 업데이트: 기존 선물의 수정 여부 확인
//    private boolean isGiftUnchanged(Gift existingGift, GiftUpdateRequest newGift) {
//        return existingGift.getName().equals(newGift.getName()) &&
//                Objects.equals(existingGift.getMessage(), newGift.getMessage()) &&
//                Objects.equals(existingGift.getPurchaseUrl(), newGift.getPurchaseUrl());
//    }

    // 보따리 업데이트: 업데이트된 이미지 저장
//    private List<GiftImage> processGiftImages(List<GiftUpdateRequest> giftRequests, List<Gift> savedGifts, Map<Long, Gift> existingGiftMap) {
//        List<GiftImage> newImages = new ArrayList<>();
//
//        for (Gift gift : savedGifts) {
//            GiftUpdateRequest giftRequest = giftRequests.stream()
//                    .filter(req -> Objects.equals(req.getId(), gift.getId()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (giftRequest == null) continue;
//            List<String> newImageUrls = giftRequest.getImageUrls();
//
//            if (newImageUrls == null || newImageUrls.isEmpty()) {
//                throw new BaseException(BaseResponseStatus.GIFT_IMAGE_REQUIRED);
//            }
//
//            // 새로운 선물의 경우: 기존 이미지가 없으므로 바로 저장
//            if (!existingGiftMap.containsKey(gift.getId())) {
//                for (int i = 0; i < newImageUrls.size(); i++) {
//                    boolean isPrimary = (i == 0); // 첫 번째 이미지를 대표 이미지로 설정
//                    newImages.add(GiftImage.createGiftImage(gift, newImageUrls.get(i), isPrimary));
//                }
//                continue;
//            }
//
//            // 기존 선물의 경우: 기존 이미지와 비교하여 추가/삭제 처리
//            List<GiftImage> existingImages = giftImageRepository.findByGiftId(gift.getId());
//            List<String> existingImageUrls = existingImages.stream()
//                    .map(GiftImage::getImageUrl)
//                    .toList();
//
//            // 삭제할 이미지 찾기
//            List<GiftImage> imagesToDelete = existingImages.stream()
//                    .filter(img -> !newImageUrls.contains(img.getImageUrl()))
//                    .toList();
//            giftImageRepository.deleteAll(imagesToDelete);
//
//            // 추가할 이미지 찾기
//            List<String> imagesToAdd = newImageUrls.stream()
//                    .filter(url -> !existingImageUrls.contains(url))
//                    .toList();
//
//            // 기존 대표 이미지 가져오기 (삭제된 경우 대비)
//            GiftImage currentPrimaryImage = existingImages.stream()
//                    .filter(GiftImage::getIsPrimary)
//                    .findFirst()
//                    .orElse(null);
//
//            String newPrimaryImageUrl = newImageUrls.get(0);
//
//            // 기존 대표 이미지가 삭제되었거나 변경되었는지 확인
//            if (currentPrimaryImage == null || !currentPrimaryImage.getImageUrl().equals(newPrimaryImageUrl)) {
//                if (currentPrimaryImage != null) {
//                    currentPrimaryImage.setPrimary(false);
//                    newImages.add(currentPrimaryImage); // 변경된 기존 이미지 추가
//                }
//
//                // 새로운 대표 이미지 추가
//                newImages.add(GiftImage.createGiftImage(gift, newPrimaryImageUrl, true));
//            }
//
//            // 나머지 추가할 이미지 저장
//            for (String imageUrl : imagesToAdd) {
//                newImages.add(GiftImage.createGiftImage(gift, imageUrl, false));
//            }
//        }
//        return newImages;
//    }



    private String generateDeliveryLink() {
        return UUID.randomUUID().toString();
    }

    private void logGiftDetails(String prefix, Gift gift) {
        log.debug("{} - [id: {}] name: {}, message: {}, purchaseUrl: {}",
                prefix, gift.getId(), gift.getName(), gift.getMessage(), gift.getPurchaseUrl());
    }
}
