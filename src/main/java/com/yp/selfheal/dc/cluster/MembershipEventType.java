package com.yp.selfheal.dc.cluster;

public enum MembershipEventType {
    JOIN,
    HEARTBEAT,
    SUSPECT,
    LEAVE,
    CONFIRMED_DEAD
}
