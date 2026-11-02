package com.yp.selfheal.dc.messaging.events;

public record NodeLeftEvent(
    String nodeId,
    String reason,
    long timestamp
) {}
