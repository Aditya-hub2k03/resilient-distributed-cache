package com.yp.selfheal.dc.ring;

import java.util.Objects;

/**
 * Represents a virtual node on the consistent hash ring pointing to a physical node.
 */
public final class VirtualNode {
    private final String nodeId;
    private final int replicaIndex;
    private final String nodeKey;

    public VirtualNode(String nodeId, int replicaIndex) {
        this.nodeId = nodeId;
        this.replicaIndex = replicaIndex;
        this.nodeKey = nodeId + "-VN#" + replicaIndex;
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getReplicaIndex() {
        return replicaIndex;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualNode that = (VirtualNode) o;
        return replicaIndex == that.replicaIndex && Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, replicaIndex);
    }

    @Override
    public String toString() {
        return nodeKey;
    }
}
