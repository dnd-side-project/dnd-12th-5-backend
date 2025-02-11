package com.picktory.domain.bundle.service;

import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import com.picktory.config.auth.AuthenticationService;
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

        // 보따리 조회 및 검증
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));
        if (!bundle.getUserId().equals(currentUser.getId())) {
            throw new BaseException(BaseResponseStatus.BUNDLE_ACCESS_DENIED);
        }

        // 기존 선물 조회 및 매핑
        List<Gift> existingGifts = giftRepository.findByBundleId(bundleId);
        Map<Long, Gift> existingGiftMap = existingGifts.stream()
                .collect(Collectors.toMap(Gift::getId, gift -> gift));

        // 디버깅: 기존 선물 목록 출력
        log.info("=== 기존 선물 목록 ===");
        existingGifts.forEach(gift ->
                log.info("ID: {}, 이름: {}, 메시지: {}, 구매링크: {}", gift.getId(), gift.getName(), gift.getMessage(), gift.getPurchaseUrl())
        );

        // 유지될 선물 목록 (ID가 존재하는 선물)
        List<Long> receivedGiftIds = request.getGifts().stream()
                .map(GiftUpdateRequest::getId)
                .filter(Objects::nonNull)
                .toList();

        // 삭제될 선물 목록 (요청에서 제외된 기존 선물)
        List<Gift> giftsToDelete = existingGifts.stream()
                .filter(gift -> !receivedGiftIds.contains(gift.getId()))
                .toList();

        // 생성될 선물 개수 계산
        long newGiftsCount = request.getGifts().size() - receivedGiftIds.size();

        // 최종 선물 개수 검증 (삭제 후에도 2개 이상인지 확인)
        long finalGiftCount = receivedGiftIds.size() + newGiftsCount;
        if (finalGiftCount < 2) {
            throw new BaseException(BaseResponseStatus.BUNDLE_MINIMUM_GIFTS_REQUIRED);
        }

        // 디버깅: 삭제될 선물 확인
        log.info("삭제될 선물 개수: {}", giftsToDelete.size());
        giftsToDelete.forEach(gift ->
                log.info("삭제 예정 선물 - ID: {}, 이름: {}, 메시지: {}, 구매링크: {}", gift.getId(), gift.getName(), gift.getMessage(), gift.getPurchaseUrl())
        );

        //
        // 여기까지 통과하면 삭제, 수정, 추가 트랜잭션 진행 가능
        //

        // 삭제할 선물 처리
        List<Long> giftIdsToDelete = giftsToDelete.stream().map(Gift::getId).toList();
        if (!giftIdsToDelete.isEmpty()) {
            giftImageRepository.deleteByGiftIds(giftIdsToDelete);
            giftRepository.deleteByIds(giftIdsToDelete);
        }

        // 수정 및 추가할 선물 처리
        List<Gift> updatedGifts = new ArrayList<>();
        List<Gift> newGifts = new ArrayList<>();

        for (GiftUpdateRequest giftUpdateRequest : request.getGifts()) {
            if (giftUpdateRequest.getId() != null && existingGiftMap.containsKey(giftUpdateRequest.getId())) {
                // 기존 선물 수정
                Gift existingGift = existingGiftMap.get(giftUpdateRequest.getId());
                existingGift.updateGift(giftUpdateRequest); // [리팩토링 필요]: 수정사항 없는 기존 선물도 재저장함. 수정사항 없는 선물은 재저장하지 않도록 검증 추가하기
                updatedGifts.add(existingGift);
            } else {
                // 새로운 선물 추가
                newGifts.add(Gift.createGift(bundle.getId(), giftUpdateRequest));
            }
        }

        // 디버깅: 수정될 기존 선물 목록 출력
        log.info("수정될 기존 선물 개수: {}", updatedGifts.size());
        updatedGifts.forEach(gift ->
                log.info("수정 예정 선물 - ID: {}, 이름: {}, 메시지: {}, 구매링크: {}", gift.getId(), gift.getName(), gift.getMessage(), gift.getPurchaseUrl())
        );

        // 디버깅: 새로 추가될 선물 목록 출력
        log.info("추가될 선물 개수: {}", newGifts.size());
        newGifts.forEach(gift ->
                log.info("추가 예정 선물 - ID: {}, 이름: {}, 메시지: {}, 구매링크: {}", gift.getId(), gift.getName(), gift.getMessage(), gift.getPurchaseUrl())
        );


        // DB에 저장할 선물 반영하기 ----------
         // 1. 기존 수정된 선물(updatedGifts) + 새로 추가될 선물(newGifts) 합치기
        List<Gift> allGiftsToSave = new ArrayList<>(updatedGifts);
        allGiftsToSave.addAll(newGifts);

         // 2. JPA에 한 번만 저장
        List<Gift> savedGifts = giftRepository.saveAll(allGiftsToSave);

        // 디버깅: 최종 저장된 선물 목록 확인
        log.info("=== 최종 저장된 선물 목록 ===");
        savedGifts.forEach(gift ->
                log.info("최종 저장 선물 - ID: {}, 이름: {}, 메시지: {}, 구매링크: {}", gift.getId(), gift.getName(), gift.getMessage(), gift.getPurchaseUrl())
        );


        // 선물 이미지 저장 및 대표 이미지 설정 -------
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


    /**
     * 배달부 캐릭터 설정
     */
    public BundleResponse updateDeliveryCharacter(Long bundleId, BundleDeliveryRequest request) {
        // 현재 로그인한 유저 가져오기
        User currentUser = authenticationService.getAuthenticatedUser();

        // 보따리 조회
        Bundle bundle = bundleRepository.findByIdAndUserId(bundleId, currentUser.getId())
                .orElseThrow(() -> new BaseException(BaseResponseStatus.BUNDLE_NOT_FOUND));

        // 배달 링크 생성 (UUID로 고유성 보장)
        String link = "/delivery/" + UUID.randomUUID().toString();

        // 배달부 캐릭터 설정 및 링크 저장
        bundle.updateDeliveryCharacter(request.getDeliveryCharacterType(), link);

        // 변경사항 DB에 저장
        Bundle savedBundle = bundleRepository.save(bundle);

        // 변경된 보따리 정보 반환
        return BundleResponse.fromEntity(savedBundle, null, null);
    }
}
