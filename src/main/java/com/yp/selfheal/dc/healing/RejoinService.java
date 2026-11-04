package com.yp.selfheal.dc.healing;

import com.yp.selfheal.dc.cluster.NodeRegistry;
import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.model.NodeStatus;
import com.yp.selfheal.dc.replication.HintedHandoffStore;
import com.yp.selfheal.dc.ring.ConsistentHashRing;
import com.yp.selfheal.dc.ring.RingRebalancer;
import com.yp.selfheal.dc.security.NodeAuthValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class RejoinService {
    private static final Logger log = LoggerFactory.getLogger(RejoinService.class);

    private final ConsistentHashRing ring;
    private final RingRebalancer ringRebalancer;
    private final NodeRegistry nodeRegistry;
    private final HintedHandoffStore handoffStore;
    private final NodeAuthValidator nodeAuthValidator;
    private final RestTemplate restTemplate = new RestTemplate();

    public RejoinService(
            ConsistentHashRing ring,
            RingRebalancer ringRebalancer,
            NodeRegistry nodeRegistry,
            HintedHandoffStore handoffStore,
            NodeAuthValidator nodeAuthValidator) {
        this.ring = ring;
        this.ringRebalancer = ringRebalancer;
        this.nodeRegistry = nodeRegistry;
        this.handoffStore = handoffStore;
        this.nodeAuthValidator = nodeAuthValidator;
    }

    public void processNodeRejoin(String recoveredNodeId, String host, int port) {
        log.info("Processing rejoin sequence for node: {}", recoveredNodeId);

        nodeRegistry.registerNode(recoveredNodeId, host, port);
        ring.addNode(recoveredNodeId);

        // Gradual rebalance
        ringRebalancer.triggerRebalance(recoveredNodeId, "REJOIN");

        // Drain deferred hinted handoffs
        deliverPendingHandoffs(recoveredNodeId, host, port);
    }

    @Async
    public void deliverPendingHandoffs(String nodeId, String host, int port) {
        List<HintedHandoffStore.Hint> hints = handoffStore.drainHints(nodeId);
        if (hints.isEmpty()) return;

        log.info("Flushing {} queued hinted handoffs to rejoining node {}", hints.size(), nodeId);

        String url = String.format("http://%s:%d/api/v1/cache/replica/put", host, port);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DC-API-KEY", nodeAuthValidator.getClusterApiKey());
        headers.set("Content-Type", "application/json");

        for (HintedHandoffStore.Hint hint : hints) {
            try {
                Map<String, Object> body = Map.of(
                        "key", hint.key(),
                        "value", hint.value(),
                        "ttlSeconds", hint.ttlSeconds() != null ? hint.ttlSeconds() : 0
                );
                restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
            } catch (Exception ex) {
                log.error("Failed to deliver handoff item for key {} to recovered node {}: {}", hint.key(), nodeId, ex.getMessage());
            }
        }
    }
}
