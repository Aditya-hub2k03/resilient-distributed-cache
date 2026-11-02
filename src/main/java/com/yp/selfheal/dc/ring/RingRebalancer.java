package com.yp.selfheal.dc.ring;

import com.yp.selfheal.dc.messaging.KafkaProducerService;
import com.yp.selfheal.dc.messaging.events.RebalanceStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Handles ring topology rebalancing when nodes join or leave.
 */
@Component
public class RingRebalancer {
    private static final Logger log = LoggerFactory.getLogger(RingRebalancer.class);

    private final ConsistentHashRing ring;
    private final KafkaProducerService kafkaProducer;
    private final int batchSize;

    public RingRebalancer(
            ConsistentHashRing ring,
            KafkaProducerService kafkaProducer,
            @Value("${dc.cluster.rebalance-batch-size:500}") int batchSize) {
        this.ring = ring;
        this.kafkaProducer = kafkaProducer;
        this.batchSize = batchSize;
    }

    public synchronized String triggerRebalance(String triggerNodeId, String action) {
        String rebalanceId = UUID.randomUUID().toString();
        log.info("Starting ring rebalance session {} triggered by {} for node {}", rebalanceId, action, triggerNodeId);

        RebalanceStartedEvent event = new RebalanceStartedEvent(
                rebalanceId,
                triggerNodeId,
                action,
                ring.size(),
                batchSize,
                System.currentTimeMillis()
        );

        kafkaProducer.sendRebalanceEvent(event);
        return rebalanceId;
    }
}
