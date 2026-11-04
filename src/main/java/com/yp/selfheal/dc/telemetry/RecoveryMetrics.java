package com.yp.selfheal.dc.telemetry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RecoveryMetrics {
    private final Counter dataLossKeysCounter;

    public RecoveryMetrics(MeterRegistry registry) {
        this.dataLossKeysCounter = Counter.builder("dc_data_loss_keys_total")
                .description("Total unrecoverable keys lost across failovers")
                .register(registry);
    }

    public void recordDataLoss(int lostKeyCount) {
        dataLossKeysCounter.increment(lostKeyCount);
    }
}
