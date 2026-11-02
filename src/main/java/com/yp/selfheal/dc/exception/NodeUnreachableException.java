package com.yp.selfheal.dc.exception;

public class NodeUnreachableException extends RuntimeException {
    public NodeUnreachableException(String nodeId) {
        super("Cluster node is currently unreachable: " + nodeId);
    }
}
