package com.yp.selfheal.dc.cluster;

import com.yp.selfheal.dc.model.NodeStatus;
import java.io.Serializable;
import java.util.Map;

public record GossipMessage(
    String senderNodeId,
    MembershipEventType eventType,
    Map<String, NodeHealthState> clusterView,
    long timestamp
) implements Serializable {

    public record NodeHealthState(
        String nodeId,
        String host,
        int port,
        NodeStatus status,
        long heartbeatSequence,
        long lastSeenTimestamp
    ) implements Serializable {}
}
