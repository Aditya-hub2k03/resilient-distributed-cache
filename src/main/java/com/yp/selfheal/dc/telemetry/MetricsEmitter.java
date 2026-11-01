package com.yp.selfheal.dc.telemetry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MetricsEmitter {
    private final Counter hitsCounter;
    private final Counter missesCounter;
    private final Counter evictionsLruCounter;

    public MetricsEmitter(MeterRegistry registry) {
        this.hitsCounter = Counter.builder("dc_cache_hits_total")
                .description("Total cache hits")
                .register(registry);
        this.missesCounter = Counter.builder("dc_cache_misses_total")
                .description("Total cache misses")
                .register(registry);
        this.evictionsLruCounter = Counter.builder("dc_evictions_total")
                .tag("reason", "lru")
                .description("Total evictions by LRU")
                .register(registry);
    }

    public void recordHit() { hitsCounter.increment(); }
    public void recordMiss() { missesCounter.increment(); }
    public void recordEvictionLru() { evictionsLruCounter.increment(); }
}
