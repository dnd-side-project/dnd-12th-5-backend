package com.picktory.domain.bundle.service;

import com.picktory.domain.bundle.dto.BundleCreateRequest;
import com.picktory.domain.bundle.dto.BundleResponse;
import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.bundle.repository.BundleRepository;
import com.picktory.domain.user.entity.User;
import com.picktory.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BundleService {

    private final BundleRepository bundleRepository;
    private final UserService userService;

    /**
     * 보따리 최초 생성
     */
    public BundleResponse createBundle(BundleCreateRequest request) {

        User currentUser = userService.getCurrentActiveUser();

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

        return BundleResponse.fromEntity(bundle);
    }
}
