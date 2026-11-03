package com.yp.selfheal.dc.cluster;

import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.model.NodeStatus;
import com.yp.selfheal.dc.repository.NodeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class HeartbeatService {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatService.class);

    private final NodeRepository nodeRepository;
    private final PhiAccrualFailureDetector failureDetector;
    private final MeterRegistry meterRegistry;
    private final RestTemplate restTemplate = new RestTemplate();

    private final AtomicLong heartbeatSeq = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> nodePhis = new ConcurrentHashMap<>();

    @Value("${dc.cluster.node-id:node-1}")
    private String selfNodeId;

    public HeartbeatService(
            NodeRepository nodeRepository,
            PhiAccrualFailureDetector failureDetector,
            MeterRegistry meterRegistry) {
        this.nodeRepository = nodeRepository;
        this.failureDetector = failureDetector;
        this.meterRegistry = meterRegistry;
    }

    public long nextSequence() {
        return heartbeatSeq.incrementAndGet();
    }

    @Scheduled(fixedRateString = "${dc.cluster.heartbeat.interval-ms:1500}")
    public void executeHeartbeatCycle() {
        List<NodeRecord> nodes = nodeRepository.findAll();

        for (NodeRecord node : nodes) {
            if (node.getNodeId().equals(selfNodeId) || node.getStatus() == NodeStatus.DEAD) {
                continue;
            }

            double currentPhi = failureDetector.phi(node.getNodeId());
            nodePhis.computeIfAbsent(node.getNodeId(), id -> {
                AtomicLong holder = new AtomicLong();
                meterRegistry.gauge("dc_heartbeat_phi", Tags.of("node", id), holder);
                return holder;
            }).set((long) (currentPhi * 100));

            // Direct ping
            boolean alive = pingNode(node);
            if (alive) {
                failureDetector.heartbeat(node.getNodeId());
                if (node.getStatus() != NodeStatus.ACTIVE) {
                    node.setStatus(NodeStatus.ACTIVE);
                    node.setLastHeartbeat(LocalDateTime.now());
                    nodeRepository.save(node);
                }
            } else if (failureDetector.isSuspect(node.getNodeId()) && node.getStatus() == NodeStatus.ACTIVE) {
                log.warn("Node {} suspected of failure (phi={})", node.getNodeId(), currentPhi);
                node.setStatus(NodeStatus.SUSPECT);
                nodeRepository.save(node);
            }
        }
    }

    private boolean pingNode(NodeRecord node) {
        try {
            String url = String.format("http://%s:%d/actuator/health", node.getHost(), node.getPort());
            var res = restTemplate.getForEntity(url, String.class);
            return res.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            return false;
        }
    }
}
