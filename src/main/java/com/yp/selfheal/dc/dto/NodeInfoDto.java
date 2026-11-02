package com.yp.selfheal.dc.dto;

import com.yp.selfheal.dc.model.NodeStatus;
import java.time.LocalDateTime;

public record NodeInfoDto(
    String nodeId,
    String host,
    int port,
    NodeStatus status,
    LocalDateTime joinedAt,
    LocalDateTime lastHeartbeat
) {}
