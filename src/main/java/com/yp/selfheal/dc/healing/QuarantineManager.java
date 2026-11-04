package com.yp.selfheal.dc.healing;

import com.yp.selfheal.dc.model.NodeStatus;
import com.yp.selfheal.dc.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QuarantineManager {
    private static final Logger log = LoggerFactory.getLogger(QuarantineManager.class);

    private final NodeRepository nodeRepository;
    private final Set<String> quarantinedNodes = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public QuarantineManager(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public void quarantineNode(String nodeId, String reason) {
        qu ビ_add(nodeId, reason);
    }

    private void qu_add(String nodeId, String reason) {
        if (quarantinedNodes.add(nodeId)) {
            log.warn("Node {} placed in QUARANTINE mode. Reason: {}", nodeId, reason);
            nodeRepository.findByNodeId(nodeId).ifPresent(n -> {
                n.setStatus(NodeStatus.QUARANTINED);
                nodeRepository.save(n);
            });
        }
    }

    public void releaseNode(String nodeId) {
        if (quarantinedNodes.remove(nodeId)) {
            log.info("Node {} released from QUARANTINE mode.", nodeId);
            nodeRepository.findByNodeId(nodeId).ifPresent(n -> {
                n.setStatus(NodeStatus.ACTIVE);
                nodeRepository.save(n);
            });
        }
    }

    public boolean isQuarantined(String nodeId) {
        return quarantinedNodes.contains(nodeId);
    }

    public Set<String> getQuarantinedNodes() {
        return Collections.unmodifiableSet(quarantinedNodes);
    }
}
