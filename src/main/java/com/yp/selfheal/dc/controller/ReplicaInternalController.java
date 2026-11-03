package com.yp.selfheal.dc.controller;

import com.yp.selfheal.dc.cache.CacheEngine;
import com.yp.selfheal.dc.dto.ApiResponse;
import com.yp.selfheal.dc.dto.CachePutRequest;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cache/replica")
@Hidden
public class ReplicaInternalController {

    private final CacheEngine cacheEngine;

    public ReplicaInternalController(CacheEngine cacheEngine) {
        this.cacheEngine = cacheEngine;
    }

    @PostMapping("/put")
    public ResponseEntity<ApiResponse<Void>> putReplica(@RequestBody CachePutRequest req) {
        cacheEngine.put(req.key(), req.value(), req.ttlSeconds());
        return ResponseEntity.ok(ApiResponse.ok("Replica written", null));
    }

    @GetMapping("/get/{key}")
    public ResponseEntity<ApiResponse<Object>> getReplica(@PathVariable String key) {
        return cacheEngine.get(key)
                .map(val -> ResponseEntity.ok(ApiResponse.ok("Replica read success", val)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Key not present")));
    }
}
