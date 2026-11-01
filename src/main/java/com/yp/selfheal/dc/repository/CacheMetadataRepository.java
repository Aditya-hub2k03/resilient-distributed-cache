package com.yp.selfheal.dc.repository;

import com.yp.selfheal.dc.model.CacheMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CacheMetadataRepository extends JpaRepository<CacheMetadata, Long> {
    Optional<CacheMetadata> findByCacheKey(String cacheKey);
    void deleteByCacheKey(String cacheKey);
}
