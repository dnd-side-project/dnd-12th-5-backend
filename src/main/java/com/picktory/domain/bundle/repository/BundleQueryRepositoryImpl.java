package com.picktory.domain.bundle.repository;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.bundle.entity.QBundle;
import com.picktory.domain.bundle.enums.BundleStatus;
import com.picktory.domain.user.entity.User;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BundleQueryRepositoryImpl implements BundleQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Bundle> findBundlesByUserDesc(User user) {
        QBundle bundle = QBundle.bundle;

        NumberExpression<Integer> sortPriority = new CaseBuilder()
                .when(bundle.status.eq(BundleStatus.COMPLETED)
                        .and(bundle.isRead.isFalse()))
                .then(0)
                .otherwise(1);

        return queryFactory
                .selectFrom(bundle)
                .where(bundle.user.eq(user))
                .orderBy(sortPriority.asc(), bundle.createdAt.desc())
                .fetch();
    }
}
