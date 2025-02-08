package com.picktory.domain.bundle.service;

import com.picktory.config.auth.AuthenticationService;
import com.picktory.domain.bundle.dto.BundleRequest;
import com.picktory.domain.bundle.dto.BundleResponse;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.gift.dto.GiftRequest;
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

        // 현재 로그인한 유저 가져오기
        User currentUser = authenticationService.getAuthenticatedUser();

        // 하루 보따리 생성 개수 제한 검사
        long todayBundleCount = bundleRepository.countByUserIdAndCreatedAtAfter(
                currentUser.getId(), LocalDateTime.now().toLocalDate().atStartOfDay()
        );

        if (todayBundleCount >= 10) {
            throw new IllegalStateException("하루에 최대 10개의 보따리만 생성할 수 있습니다.");
        }

        // 보따리 이름 검증
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("보따리 이름을 입력하세요.");
        }

        // 보따리 디자인 타입 검증
        if (request.getDesignType() == null) {
            throw new IllegalArgumentException("보따리 디자인을 선택하세요.");
        }

        // 보따리에 담긴 선물이 2개 미만인 경우 예외 처리
        if (request.getGifts() == null || request.getGifts().size() < 2) {
            throw new IllegalArgumentException("보따리는 최소 2개의 선물을 포함해야 합니다.");
        }

        // 보따리 저장
        Bundle bundle = bundleRepository.save(
                Bundle.builder()
                        .userId(currentUser.getId())
                        .name(request.getName())
                        .designType(request.getDesignType())
                        .deliveryCharacterType(null)
                        .status(BundleStatus.DRAFT)
                        .isRead(false)
                        .build()
        );

        // Gift 및 GiftImage 저장 로직
        List<Gift> savedGifts = new ArrayList<>();
        List<GiftImage> savedGiftImages = new ArrayList<>();

        if (!CollectionUtils.isEmpty(request.getGifts())) {
            List<Gift> gifts = new ArrayList<>();
            List<GiftImage> giftImages = new ArrayList<>();

            for (GiftRequest giftRequest : request.getGifts()) {
                Gift gift = Gift.createGift(bundle.getId(), giftRequest);
                gifts.add(gift);

                if (!CollectionUtils.isEmpty(giftRequest.getImageUrls())) {
                    for (String imageUrl : giftRequest.getImageUrls()) {
                        giftImages.add(GiftImage.createGiftImage(gift.getId(), imageUrl, false));
                    }
                }
            }

            savedGifts = giftRepository.saveAll(gifts); // 배치 저장 (디비에 한번에 저장)
            savedGiftImages = giftImageRepository.saveAll(giftImages); // 배치 저장 (디비에 한번에 저장)
        }

        // 응답 반환 (보따리 + 선물 + 이미지)
            //
            // 프론트에서 필요한 응답 확인 후 축소 가능
            //
        return BundleResponse.fromEntity(bundle, savedGifts, savedGiftImages);
    }
}
