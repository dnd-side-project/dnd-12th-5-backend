package com.picktory.domain.response.repository;

import com.picktory.domain.response.entity.Response;
import java.util.List;

public interface ResponseRepositoryCustom {
    List<Response> findAllByBundleIdAndGiftIds(Long bundleId, List<Long> giftIds);
}