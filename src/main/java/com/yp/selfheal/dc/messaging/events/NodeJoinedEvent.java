package com.yp.selfheal.dc.messaging.events;

public record NodeJoinedEvent(
    String nodeId,
    String host,
    int port,
    long timestamp
) {}
