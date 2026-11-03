package com.yp.selfheal.dc.cluster;

import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.model.NodeStatus;
import com.yp.selfheal.dc.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GossipService {
    private static final Logger log = LoggerFactory.getLogger(GossipService.class);

    private final NodeRepository nodeRepository;
    private final HeartbeatService heartbeatService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${dc.cluster.node-id:node-1}")
    private String selfNodeId;

    @Value("${dc.cluster.host:localhost}")
    private String selfHost;

    @Value("${server.port:8080}")
    private int selfPort;

    @Value("${dc.security.api-key:secret-cluster-key}")
    private String clusterApiKey;

    private final Map<String, GossipMessage.NodeHealthState> clusterView = new ConcurrentHashMap<>();

    public GossipService(NodeRepository nodeRepository, HeartbeatService heartbeatService) {
        this.nodeRepository = nodeRepository;
        this.heartbeatService = heartbeatService;
    }

    @Scheduled(fixedRateString = "${dc.cluster.gossip.interval-ms:2000}")
    public void disseminateGossip() {
        refreshSelfState();

        List<NodeRecord> candidates = nodeRepository.findAll().stream()
                .filter(n -> !n.getNodeId().equals(selfNodeId) && n.getStatus() != NodeStatus.DEAD)
                .toList();

        if (candidates.isEmpty()) return;

        // Pick random gossip peer
        NodeRecord target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        GossipMessage msg = new GossipMessage(
                selfNodeId,
                MembershipEventType.HEARTBEAT,
                new HashMap<>(clusterView),
                System.currentTimeMillis()
        );

        try {
            String url = String.format("http://%s:%d/api/v1/cluster/gossip", target.getHost(), target.getPort());
            restTemplate.postForObject(url, msg, Void.class);
        } catch (Exception ex) {
            log.debug("Gossip delivery failed to {}: {}", target.getNodeId(), ex.getMessage());
        }
    }

    public void processGossipMessage(GossipMessage msg) {
        msg.clusterView().forEach((nodeId, incomingState) -> {
            clusterView.compute(nodeId, (k, existingState) -> {
                if (existingState == null || incomingState.heartbeatSequence() > existingState.heartbeatSequence()) {
                    return incomingState;
                }
                return existingState;
            });
        });
    }

    private void refreshSelfState() {
        clusterView.put(selfNodeId, new GossipMessage.NodeHealthState(
                selfNodeId, selfHost, selfPort, NodeStatus.ACTIVE,
                heartbeatService.nextSequence(), System.currentTimeMillis()
        ));
    }
}
