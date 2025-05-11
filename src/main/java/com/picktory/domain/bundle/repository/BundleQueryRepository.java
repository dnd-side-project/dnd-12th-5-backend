package com.picktory.domain.bundle.repository;

import com.picktory.domain.bundle.entity.Bundle;
import com.picktory.domain.user.entity.User;

import java.util.List;

public interface BundleQueryRepository {
    List<Bundle> findBundlesByUserDesc(User user);
}
