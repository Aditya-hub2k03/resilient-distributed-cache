package com.yp.selfheal.dc.replication;

import com.yp.selfheal.dc.ring.ConsistentHashRing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReplicaRouter {

    private final ConsistentHashRing ring;
    private final int replicationFactor;

    public ReplicaRouter(
            ConsistentHashRing ring,
            @Value("${dc.cluster.replication.n:3}") int replicationFactor) {
        this.ring = ring;
        this.replicationFactor = replicationFactor;
    }

    /**
     * Determines the N successor replica owner nodes for a key.
     */
    public List<String> getReplicaNodes(String key) {
        return ring.getSuccessorNodes(key, replicationFactor);
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }
}
