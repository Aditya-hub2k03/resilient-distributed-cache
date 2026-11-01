package com.yp.selfheal.dc.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "key_stats")
public class KeyStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", unique = true, nullable = false, length = 255)
    private String cacheKey;

    @Column(name = "hit_count", nullable = false)
    private Long hitCount = 0L;

    @Column(name = "miss_count", nullable = false)
    private Long missCount = 0L;

    @Column(name = "recompute_cost_ms")
    private Integer recomputeCostMs = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    public KeyStats() {}

    public KeyStats(String cacheKey) {
        this.cacheKey = cacheKey;
        this.hitCount = 0L;
        this.missCount = 0L;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCacheKey() { return cacheKey; }
    public Long getHitCount() { return hitCount; }
    public void incrementHits() { this.hitCount++; this.lastAccessedAt = LocalDateTime.now(); }
    public Long getMissCount() { return missCount; }
    public void incrementMisses() { this.missCount++; this.lastAccessedAt = LocalDateTime.now(); }
    public Integer getRecomputeCostMs() { return recomputeCostMs; }
    public void setRecomputeCostMs(Integer recomputeCostMs) { this.recomputeCostMs = recomputeCostMs; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
}
