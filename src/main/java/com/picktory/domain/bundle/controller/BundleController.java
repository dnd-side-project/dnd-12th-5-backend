package com.picktory.domain.bundle.controller;

import com.picktory.common.BaseResponse;

import com.picktory.common.BaseResponseStatus;
import com.picktory.config.auth.AuthenticationService;
import com.picktory.domain.bundle.dto.*;

import com.picktory.domain.bundle.dto.BundleDeliveryRequest;
import com.picktory.domain.bundle.dto.BundleRequest;
import com.picktory.domain.bundle.dto.BundleResponse;
import com.picktory.domain.bundle.dto.BundleUpdateRequest;

import com.picktory.domain.bundle.service.BundleService;
import com.picktory.domain.gift.dto.DraftGiftsResponse;
import com.picktory.domain.gift.dto.GiftDetailResponse;
import com.picktory.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bundles")
@RequiredArgsConstructor
public class BundleController {

    private final BundleService bundleService;
    private final AuthenticationService authenticationService;

    /**
     * 보따리 최초 생성 API
     */
    @PostMapping
    public ResponseEntity<BundleResponse> createBundle(@Valid @RequestBody BundleRequest request) {
        BundleResponse response = bundleService.createBundle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 보따리 업데이트 API
     */
    @PutMapping("/{bundleId}")
    public ResponseEntity<BaseResponse<BundleResponse>> updateBundle(
            @PathVariable Long bundleId,
            @Valid @RequestBody BundleUpdateRequest request
    ) {
        BundleResponse response = bundleService.updateBundle(bundleId, request);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }


    /**
     * 보따리 목록 조회 API
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<BundleListResponse>>> getBundles() {
        User currentUser = authenticationService.getAuthenticatedUser();
        List<BundleListResponse> bundles = bundleService.getMyBundles(currentUser);
        return ResponseEntity.ok(new BaseResponse<>(bundles));
    }

    /**

     * 보따리 메인 목록 조회 API (최신 8개)
     */
    @GetMapping("/main")
    public ResponseEntity<BaseResponse<List<BundleMainListResponse>>> getMainBundles() {
        List<BundleMainListResponse> bundles = bundleService.getUserMainBundles();
        return ResponseEntity.ok(new BaseResponse<>(bundles));
    }

    /**

}


     * 배달부 캐릭터 설정 API
     */
    @PutMapping("/{id}/delivery")
    public ResponseEntity<BundleResponse> updateDeliveryCharacter(
            @PathVariable Long id,
            @Valid @RequestBody BundleDeliveryRequest request
    ) {
        return ResponseEntity.ok(bundleService.updateDeliveryCharacter(id, request));
    }

    /**
     * 보따리 삭제 API
     */
    @DeleteMapping("/{bundleId}")
    public ResponseEntity<Void> deleteBundle(@PathVariable Long bundleId) {
        bundleService.deleteBundle(bundleId);
        return ResponseEntity.noContent().build(); // 204
    }

    /**
     * 보따리 결과 조회 API
     */
    @GetMapping("/{id}/result")
    public ResponseEntity<BaseResponse<BundleResultResponse>> getBundleResult(@PathVariable Long id) {
        BundleResultResponse response = bundleService.getBundleResult(id);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }

    /**
     * 보따리 개별 선물 조회 API
     */
    @GetMapping("/{bundleId}/gifts/{giftId}")
    public ResponseEntity<GiftDetailResponse> getGift(
            @PathVariable Long bundleId,
            @PathVariable Long giftId
    ) {
        return ResponseEntity.ok(bundleService.getGift(bundleId, giftId));
    }

    /**
     * 임시 저장된 보따리의 선물 목록 조회 API
     */
    @GetMapping("/{id}/gifts")
    public ResponseEntity<DraftGiftsResponse> getDraftGifts(@PathVariable Long id) {
        return ResponseEntity.ok(bundleService.getDraftGifts(id));
    }
  
    /**
     * 보따리 조회 API (간이 조회)
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<BundleSummaryResponse>> getBundle(@PathVariable Long id) {
        BundleSummaryResponse response = bundleService.getBundle(id);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }

   /**
     * 보따리 이름 업데이트 API
     */
   @PatchMapping("/{bundleId}")
   public ResponseEntity<BaseResponse<Void>> updateBundleName(
           @PathVariable Long bundleId,
           @Valid @RequestBody BundleNameUpdateRequest request
   ) {
       bundleService.updateBundleName(bundleId, request);
       return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS));
   }
}

