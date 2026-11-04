package com.yp.selfheal.dc.healing;

import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.model.NodeStatus;
import com.yp.selfheal.dc.repository.NodeRepository;
import com.yp.selfheal.dc.ring.ConsistentHashRing;
import com.yp.selfheal.dc.ring.RingRebalancer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class FailoverService {
    private static final Logger log = LoggerFactory.getLogger(FailoverService.class);

    private final ConsistentHashRing ring;
    private final RingRebalancer ringRebalancer;
    private final NodeRepository nodeRepository;
    private final Timer failoverDurationTimer;

    public FailoverService(
            ConsistentHashRing ring,
            RingRebalancer ringRebalancer,
            NodeRepository nodeRepository,
            MeterRegistry meterRegistry) {
        this.ring = ring;
        this.ringRebalancer = ringRebalancer;
        this.nodeRepository = nodeRepository;
        this.failoverDurationTimer = Timer.builder("dc_failover_duration_seconds")
                .description("Time taken to complete failover and ring range reassignment")
                .register(meterRegistry);
    }

    public synchronized void failoverDeadNode(String deadNodeId) {
        Instant start = Instant.now();
        log.error("Initiating failover for confirmed dead node: {}", deadNodeId);

        // 1. Remove from consistent hash ring
        ring.removeNode(deadNodeId);

        // 2. Mark dead in MySQL registry
        nodeRepository.findByNodeId(deadNodeId).ifPresent(n -> {
            n.setStatus(NodeStatus.DEAD);
            nodeRepository.save(n);
        });

        // 3. Rebalance range tokens
        ringRebalancer.triggerRebalance(deadNodeId, "FAILOVER");

        Duration elapsed = Duration.between(start, Instant.now());
        failoverDurationTimer.record(elapsed);
        log.info("Failover completed for node {} in {}ms", deadNodeId, elapsed.toMillis());
    }
}
