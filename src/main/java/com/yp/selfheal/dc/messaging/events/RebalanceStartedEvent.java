package com.yp.selfheal.dc.messaging.events;

public record RebalanceStartedEvent(
    String rebalanceId,
    String triggerNodeId,
    String action,
    int totalNodes,
    int batchSize,
    long timestamp
) {}
