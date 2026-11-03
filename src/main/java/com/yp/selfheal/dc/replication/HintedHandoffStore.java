package com.yp.selfheal.dc.replication;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory hinted handoff storage for temporarily unavailable replicas.
 */
@Component
public class HintedHandoffStore {
    private static final Logger log = LoggerFactory.getLogger(HintedHandoffStore.class);

    public record Hint(String targetNodeId, String key, Object value, Long ttlSeconds, long timestamp) {}

    private final Map<String, ConcurrentLinkedQueue<Hint>> hintsByTarget = new ConcurrentHashMap<>();
    private final AtomicInteger pendingHintsCount = new AtomicInteger(0);

    public HintedHandoffStore(MeterRegistry meterRegistry) {
        meterRegistry.gauge("dc_handoff_pending_total", pendingHintsCount);
    }

    public void storeHint(String targetNodeId, String key, Object value, Long ttlSeconds) {
        Hint hint = new Hint(targetNodeId, key, value, ttlSeconds, System.currentTimeMillis());
        hintsByTarget.computeIfAbsent(targetNodeId, k -> new ConcurrentLinkedQueue<>()).add(hint);
        pendingHintsCount.incrementAndGet();
        log.warn("Stored hinted handoff write for offline target node {} (key: {})", targetNodeId, key);
    }

    public List<Hint> drainHints(String targetNodeId) {
        ConcurrentLinkedQueue<Hint> queue = hintsByTarget.remove(targetNodeId);
        if (queue == null) return Collections.emptyList();

        List<Hint> hints = new ArrayList<>(queue);
        pendingHintsCount.addAndGet(-hints.size());
        return hints;
    }

    public int getPendingCount() {
        return pendingHintsCount.get();
    }
}
