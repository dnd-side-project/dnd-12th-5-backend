package com.picktory.domain.bundle.repository;

import com.picktory.domain.bundle.entity.Bundle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BundleRepository extends JpaRepository<Bundle, Long> {
}
