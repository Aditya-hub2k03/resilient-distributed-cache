package com.yp.selfheal.dc.cache;

import java.time.Instant;

public class CacheEntry<V> {
    private final String key;
    private final V value;
    private final Instant createdAt;
    private final Instant expiresAt; // null for persistent

    public CacheEntry(String key, V value, Long ttlSeconds) {
        this.key = key;
        this.value = value;
        this.createdAt = Instant.now();
        this.expiresAt = (ttlSeconds != null && ttlSeconds > 0) ? this.createdAt.plusSeconds(ttlSeconds) : null;
    }

    public String getKey() { return key; }
    public V getValue() { return value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
