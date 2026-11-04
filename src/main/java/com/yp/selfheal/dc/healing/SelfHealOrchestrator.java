package com.yp.selfheal.dc.healing;

import com.yp.selfheal.dc.cluster.PhiAccrualFailureDetector;
import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.model.NodeStatus;
import com.yp.selfheal.dc.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SelfHealOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(SelfHealOrchestrator.class);

    private final PhiAccrualFailureDetector failureDetector;
    private final NodeRepository nodeRepository;
    private final QuarantineManager quarantineManager;
    private final FailoverService failoverService;

    @Value("${dc.cluster.node-id:node-1}")
    private String selfNodeId;

    @Value("${dc.healing.quarantine.phi-threshold:10.0}")
    private double quarantinePhiThreshold;

    @Value("${dc.healing.failover.phi-threshold:14.0}")
    private double failoverPhiThreshold;

    public SelfHealOrchestrator(
            PhiAccrualFailureDetector failureDetector,
            NodeRepository nodeRepository,
            QuarantineManager quarantineManager,
            FailoverService failoverService) {
        this.failureDetector = failureDetector;
        this.nodeRepository = nodeRepository;
        this.quarantineManager = quarantineManager;
        this.failoverService = failoverService;
    }

    @Scheduled(fixedDelayString = "${dc.healing.orchestration.interval-ms:3000}")
    public void evaluateClusterStateAndHeal() {
        List<NodeRecord> nodes = nodeRepository.findAll();

        for (NodeRecord node : nodes) {
            String nodeId = node.getNodeId();
            if (nodeId.equals(selfNodeId) || node.getStatus() == NodeStatus.DEAD) {
                continue;
            }

            double phi = failureDetector.phi(nodeId);

            // Phase 4 Self-Healing Rule 1: High suspicion -> Quarantine
            if (phi >= quarantinePhiThreshold && phi < failoverPhiThreshold && !quarantineManager.isQuarantined(nodeId)) {
                quarantineManager.quarantineNode(nodeId, "Phi suspicion level exceeded quarantine threshold: " + phi);
            }

            // Phase 4 Self-Healing Rule 2: Extreme suspicion -> Failover & Ring Eviction
            if (phi >= failoverPhiThreshold) {
                failoverService.failoverDeadNode(nodeId);
            }
        }
    }
}
