package com.yp.selfheal.dc.cache;

import com.yp.selfheal.dc.telemetry.MetricsEmitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class CacheEngine {
    private final LocalStore<Object> store;
    private final EvictionPolicy evictionPolicy;
    private final MetricsEmitter metricsEmitter;
    private final int capacity;
    private final ReentrantLock putLock = new ReentrantLock();

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    public CacheEngine(
            LocalStore<Object> store,
            EvictionPolicy evictionPolicy,
            MetricsEmitter metricsEmitter,
            @Value("${dc.cache.max-capacity:1000}") int capacity) {
        this.store = store;
        this.evictionPolicy = evictionPolicy;
        this.metricsEmitter = metricsEmitter;
        this.capacity = capacity;
    }

    public Optional<Object> get(String key) {
        Optional<CacheEntry<Object>> entryOpt = store.get(key);
        if (entryOpt.isPresent()) {
            CacheEntry<Object> entry = entryOpt.get();
            if (entry.isExpired()) {
                store.remove(key);
                evictionPolicy.recordRemove(key);
                missCount.incrementAndGet();
                metricsEmitter.recordMiss();
                return Optional.empty();
            }
            evictionPolicy.recordAccess(key);
            hitCount.incrementAndGet();
            metricsEmitter.recordHit();
            return Optional.of(entry.getValue());
        }
        missCount.incrementAndGet();
        metricsEmitter.recordMiss();
        return Optional.empty();
    }

    public void put(String key, Object value, Long ttlSeconds) {
        putLock.lock();
        try {
            if (!store.containsKey(key) && store.size() >= capacity) {
                String victim = evictionPolicy.evictKey();
                if (victim != null) {
                    store.remove(victim);
                    evictionCount.incrementAndGet();
                    metricsEmitter.recordEvictionLru();
                }
            }
            CacheEntry<Object> entry = new CacheEntry<>(key, value, ttlSeconds);
            store.put(key, entry);
            evictionPolicy.recordPut(key);
        } finally {
            putLock.unlock();
        }
    }

    public boolean delete(String key) {
        CacheEntry<Object> removed = store.remove(key);
        if (removed != null) {
            evictionPolicy.recordRemove(key);
            return true;
        }
        return false;
    }

    public long getHitCount() { return hitCount.get(); }
    public long getMissCount() { return missCount.get(); }
    public long getEvictionCount() { return evictionCount.get(); }
    public int getCurrentSize() { return store.size(); }
    public int getCapacity() { return capacity; }
}
