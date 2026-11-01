package com.yp.selfheal.dc.controller;

import com.yp.selfheal.dc.dto.ApiResponse;
import com.yp.selfheal.dc.dto.CachePutRequest;
import com.yp.selfheal.dc.dto.CacheStatsResponse;
import com.yp.selfheal.dc.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cache")
@Tag(name = "Cache Operations", description = "Endpoints for CRUD cache management and metrics")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get cached value", description = "Retrieves an entry by key from L1 cache")
    public ResponseEntity<ApiResponse<Object>> get(@PathVariable String key) {
        Object val = cacheService.get(key);
        return ResponseEntity.ok(ApiResponse.ok("Cache entry fetched", val));
    }

    @PostMapping
    @Operation(summary = "Store cache value", description = "Saves an entry into L1 cache with optional TTL")
    public ResponseEntity<ApiResponse<Void>> put(@Valid @RequestBody CachePutRequest request) {
        cacheService.put(request.key(), request.value(), request.ttlSeconds());
        return ResponseEntity.ok(ApiResponse.ok("Cache entry persisted successfully", null));
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Delete cache entry", description = "Invalidates and deletes a cached key")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String key) {
        cacheService.delete(key);
        return ResponseEntity.ok(ApiResponse.ok("Cache entry evicted successfully", null));
    }

    @GetMapping("/stats")
    @Operation(summary = "Cache telemetry stats", description = "Retrieves hit, miss, eviction and capacity stats")
    public ResponseEntity<ApiResponse<CacheStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok("Telemetry stats fetched", cacheService.getStats()));
    }
}
