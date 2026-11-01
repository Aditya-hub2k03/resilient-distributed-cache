package com.yp.selfheal.dc.cache;

import com.yp.selfheal.dc.telemetry.MetricsEmitter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CacheEngineTest {

    private CacheEngine cacheEngine;
    private LocalStore<Object> localStore;
    private EvictionPolicy evictionPolicy;

    @BeforeEach
    void setUp() {
        localStore = new LocalStore<>();
        evictionPolicy = new LruEvictionPolicy();
        MetricsEmitter metricsEmitter = new MetricsEmitter(new SimpleMeterRegistry());
        cacheEngine = new CacheEngine(localStore, evictionPolicy, metricsEmitter, 3);
    }

    @Test
    void testBasicPutAndGet() {
        cacheEngine.put("user:1", "Alice", null);
        Optional<Object> result = cacheEngine.get("user:1");

        assertTrue(result.isPresent());
        assertEquals("Alice", result.get());
        assertEquals(1, cacheEngine.getHitCount());
    }

    @Test
    void testLruEvictionWhenCapacityExceeded() {
        cacheEngine.put("k1", "v1", null);
        cacheEngine.put("k2", "v2", null);
        cacheEngine.put("k3", "v3", null);

        // Access k1 to make it most recently used
        cacheEngine.get("k1");

        // Put k4 -> should evict k2
        cacheEngine.put("k4", "v4", null);

        assertTrue(cacheEngine.get("k1").isPresent());
        assertTrue(cacheEngine.get("k3").isPresent());
        assertTrue(cacheEngine.get("k4").isPresent());
        assertFalse(cacheEngine.get("k2").isPresent());
        assertEquals(1, cacheEngine.getEvictionCount());
    }

    @Test
    void testTtlExpirationOnAccess() throws InterruptedException {
        // TTL 1 second
        cacheEngine.put("tempKey", "temporary", 1L);
        assertTrue(cacheEngine.get("tempKey").isPresent());

        Thread.sleep(1100);
        assertFalse(cacheEngine.get("tempKey").isPresent());
    }
}
