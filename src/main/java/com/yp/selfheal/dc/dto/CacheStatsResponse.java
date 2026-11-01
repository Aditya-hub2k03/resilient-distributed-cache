package com.yp.selfheal.dc.dto;

public record CacheStatsResponse(
    long hits,
    long misses,
    long evictions,
    int currentSize,
    int capacity,
    double hitRate
) {}
