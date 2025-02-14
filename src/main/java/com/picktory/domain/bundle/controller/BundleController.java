package com.picktory.domain.bundle.controller;

import com.picktory.common.BaseResponse;

import com.picktory.domain.bundle.dto.*;

import com.picktory.domain.bundle.dto.BundleDeliveryRequest;
import com.picktory.domain.bundle.dto.BundleRequest;
import com.picktory.domain.bundle.dto.BundleResponse;
import com.picktory.domain.bundle.dto.BundleUpdateRequest;

import com.picktory.domain.bundle.service.BundleService;
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

    /**
     * 보따리 최초 생성 API
     */
    @PostMapping
    public ResponseEntity<BundleResponse> createBundle(@Valid @RequestBody BundleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bundleService.createBundle(request));
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
        List<BundleListResponse> bundles = bundleService.getUserBundles();
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
}

