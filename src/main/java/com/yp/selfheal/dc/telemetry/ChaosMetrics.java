package com.yp.selfheal.dc.telemetry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ChaosMetrics {
    private final Counter simulatedPartitionsCounter;
    private final Counter simulatedKillsCounter;

    public ChaosMetrics(MeterRegistry registry) {
        this.simulatedPartitionsCounter = Counter.builder("dc_chaos_simulated_partitions_total")
                .description("Simulated network partition incidents")
                .register(registry);
        this.simulatedKillsCounter = Counter.builder("dc_chaos_simulated_kills_total")
                .description("Simulated node process terminations")
                .register(registry);
    }

    public void incrementPartitions() { simulatedPartitionsCounter.increment(); }
    public void incrementKills() { simulatedKillsCounter.increment(); }
}
