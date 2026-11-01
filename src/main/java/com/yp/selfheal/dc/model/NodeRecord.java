package com.yp.selfheal.dc.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nodes")
public class NodeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", unique = true, nullable = false, length = 64)
    private String nodeId;

    @Column(nullable = false, length = 128)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NodeStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    public NodeRecord() {}

    public NodeRecord(String nodeId, String host, Integer port, NodeStatus status) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.status = status;
        this.joinedAt = LocalDateTime.now();
        this.lastHeartbeat = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
}
