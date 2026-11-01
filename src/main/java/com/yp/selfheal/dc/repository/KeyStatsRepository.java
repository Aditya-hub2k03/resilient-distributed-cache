package com.yp.selfheal.dc.repository;

import com.yp.selfheal.dc.model.KeyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KeyStatsRepository extends JpaRepository<KeyStats, Long> {
    Optional<KeyStats> findByCacheKey(String cacheKey);
}
