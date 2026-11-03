package com.yp.selfheal.dc.replication;

import com.yp.selfheal.dc.exception.QuorumNotMetException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class QuorumCoordinator {

    private final int replicationFactorN;
    private final int writeQuorumW;
    private final int readQuorumR;
    private final Counter quorumFailuresCounter;

    public QuorumCoordinator(
            @Value("${dc.cluster.replication.n:3}") int replicationFactorN,
            @Value("${dc.cluster.replication.w:2}") int writeQuorumW,
            @Value("${dc.cluster.replication.r:2}") int readQuorumR,
            MeterRegistry meterRegistry) {
        this.replicationFactorN = replicationFactorN;
        this.writeQuorumW = writeQuorumW;
        this.readQuorumR = readQuorumR;

        if (writeQuorumW + readQuorumR <= replicationFactorN) {
            throw new IllegalArgumentException("Quorum invalid: R + W must be > N for strict consistency");
        }

        this.quorumFailuresCounter = Counter.builder("dc_quorum_failures_total")
                .description("Total operations failing to meet quorum requirement")
                .register(meterRegistry);
    }

    public void validateWriteQuorum(int successfulAcks) {
        if (successfulAcks < writeQuorumW) {
            quorumFailuresCounter.increment();
            throw new QuorumNotMetException("Write quorum not reached! Required: " + writeQuorumW + ", Achieved: " + successfulAcks);
        }
    }

    public void validateReadQuorum(int successfulAcks) {
        if (successfulAcks < readQuorumR) {
            quorumFailuresCounter.increment();
            throw new QuorumNotMetException("Read quorum not reached! Required: " + readQuorumR + ", Achieved: " + successfulAcks);
        }
    }

    public int getReplicationFactorN() { return replicationFactorN; }
    public int getWriteQuorumW() { return writeQuorumW; }
    public int getReadQuorumR() { return readQuorumR; }
}
