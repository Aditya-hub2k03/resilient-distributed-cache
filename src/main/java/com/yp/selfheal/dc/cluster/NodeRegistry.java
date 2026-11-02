package com.yp.selfheal.dc.cluster;

import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.model.NodeStatus;
import com.yp.selfheal.dc.repository.NodeRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class NodeRegistry {

    private final NodeRepository nodeRepository;

    public NodeRegistry(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public NodeRecord registerNode(String nodeId, String host, int port) {
        return nodeRepository.findByNodeId(nodeId).map(existing -> {
            existing.setHost(host);
            existing.setPort(port);
            existing.setStatus(NodeStatus.ACTIVE);
            existing.setLastHeartbeat(LocalDateTime.now());
            return nodeRepository.save(existing);
        }).orElseGet(() -> {
            NodeRecord record = new NodeRecord(nodeId, host, port, NodeStatus.ACTIVE);
            return nodeRepository.save(record);
        });
    }

    public Optional<NodeRecord> getNode(String nodeId) {
        return nodeRepository.findByNodeId(nodeId);
    }

    public List<NodeRecord> getAllActiveNodes() {
        return nodeRepository.findAll().stream()
                .filter(n -> n.getStatus() == NodeStatus.ACTIVE)
                .toList();
    }

    public void updateStatus(String nodeId, NodeStatus status) {
        nodeRepository.findByNodeId(nodeId).ifPresent(n -> {
            n.setStatus(status);
            nodeRepository.save(n);
        });
    }
}
