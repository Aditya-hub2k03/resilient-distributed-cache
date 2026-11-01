package com.yp.selfheal.dc.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cache_metadata")
public class CacheMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", unique = true, nullable = false, length = 255)
    private String cacheKey;

    @Column(name = "value_hash", length = 64)
    private String valueHash;

    @Column(name = "size_bytes")
    private Integer sizeBytes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public CacheMetadata() {}

    public CacheMetadata(String cacheKey, String valueHash, Integer sizeBytes, LocalDateTime expiresAt) {
        this.cacheKey = cacheKey;
        this.valueHash = valueHash;
        this.sizeBytes = sizeBytes;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }
    public String getValueHash() { return valueHash; }
    public void setValueHash(String valueHash) { this.valueHash = valueHash; }
    public Integer getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Integer sizeBytes) { this.sizeBytes = sizeBytes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
