package com.picktory.domain.response.repository;

import com.picktory.domain.response.entity.QResponse;
import com.picktory.domain.response.entity.Response;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RequiredArgsConstructor
public class ResponseRepositoryImpl implements ResponseRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QResponse response = QResponse.response;

    @Override
    public List<Response> findAllByBundleIdAndGiftIds(Long bundleId, List<Long> giftIds) {
        return queryFactory
                .selectFrom(response)
                .where(
                        bundleIdEq(bundleId),
                        giftIdsIn(giftIds)
                )
                .orderBy(response.createdAt.desc())
                .fetch();
    }

    private BooleanExpression bundleIdEq(Long bundleId) {
        return bundleId != null ? response.bundleId.eq(bundleId) : null;
    }

    private BooleanExpression giftIdsIn(List<Long> giftIds) {
        return giftIds != null && !giftIds.isEmpty() ? response.giftId.in(giftIds) : null;
    }
}