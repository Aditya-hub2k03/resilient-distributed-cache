package com.yp.selfheal.dc.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id")
    private Long nodeId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    public AuditLog() {}

    public AuditLog(Long nodeId, String eventType, String detail) {
        this.nodeId = nodeId;
        this.eventType = eventType;
        this.detail = detail;
        this.eventTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getNodeId() { return nodeId; }
    public String getEventType() { return eventType; }
    public String getDetail() { return detail; }
    public LocalDateTime getEventTime() { return eventTime; }
}
