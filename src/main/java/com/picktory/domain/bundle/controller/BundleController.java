package com.picktory.domain.bundle.controller;

import com.picktory.domain.bundle.dto.BundleRequest;
import com.picktory.domain.bundle.dto.BundleResponse;
import com.picktory.domain.bundle.service.BundleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * 배달부 캐릭터 설정 API
     */
    @PutMapping("/{id}/delivery")
    public ResponseEntity<BundleResponse> updateDeliveryCharacter(
            @PathVariable Long id,
            @Valid @RequestBody BundleRequest request
    ) {
        return ResponseEntity.ok(bundleService.updateDeliveryCharacter(id, request));
    }
}
