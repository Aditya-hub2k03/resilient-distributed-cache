package com.yp.selfheal.dc.service;

import com.yp.selfheal.dc.cluster.NodeRegistry;
import com.yp.selfheal.dc.messaging.KafkaProducerService;
import com.yp.selfheal.dc.messaging.events.NodeJoinedEvent;
import com.yp.selfheal.dc.messaging.events.NodeLeftEvent;
import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.model.NodeStatus;
import com.yp.selfheal.dc.ring.ConsistentHashRing;
import com.yp.selfheal.dc.ring.RingRebalancer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NodeLifecycleService {
    private static final Logger log = LoggerFactory.getLogger(NodeLifecycleService.class);

    private final NodeRegistry nodeRegistry;
    private final ConsistentHashRing ring;
    private final RingRebalancer ringRebalancer;
    private final KafkaProducerService kafkaProducer;

    @Value("${dc.cluster.node-id:node-1}")
    private String selfNodeId;

    @Value("${dc.cluster.host:localhost}")
    private String selfHost;

    @Value("${server.port:8080}")
    private int selfPort;

    public NodeLifecycleService(
            NodeRegistry nodeRegistry,
            ConsistentHashRing ring,
            RingRebalancer ringRebalancer,
            KafkaProducerService kafkaProducer) {
        this.nodeRegistry = nodeRegistry;
        this.ring = ring;
        this.ringRebalancer = ringRebalancer;
        this.kafkaProducer = kafkaProducer;
    }

    @PostConstruct
    public void bootstrapNode() {
        log.info("Bootstrapping cluster node: id={}, host={}, port={}", selfNodeId, selfHost, selfPort);
        registerAndJoin(selfNodeId, selfHost, selfPort);

        // Load existing cluster nodes from MySQL into consistent hash ring
        List<NodeRecord> existingNodes = nodeRegistry.getAllActiveNodes();
        for (NodeRecord node : existingNodes) {
            ring.addNode(node.getNodeId());
        }
    }

    public NodeRecord registerAndJoin(String nodeId, String host, int port) {
        NodeRecord record = nodeRegistry.registerNode(nodeId, host, port);
        ring.addNode(nodeId);

        kafkaProducer.sendNodeJoined(new NodeJoinedEvent(nodeId, host, port, System.currentTimeMillis()));
        ringRebalancer.triggerRebalance(nodeId, "JOIN");
        return record;
    }

    public void deregisterAndLeave(String nodeId, String reason) {
        nodeRegistry.updateStatus(nodeId, NodeStatus.DEAD);
        ring.removeNode(nodeId);

        kafkaProducer.sendNodeLeft(new NodeLeftEvent(nodeId, reason, System.currentTimeMillis()));
        ringRebalancer.triggerRebalance(nodeId, "LEAVE");
    }

    public String getSelfNodeId() {
        return selfNodeId;
    }
}
