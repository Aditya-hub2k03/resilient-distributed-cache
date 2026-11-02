package com.yp.selfheal.dc.dto;

import java.util.Map;
import java.util.Set;

public record RingStateDto(
    int totalPhysicalNodes,
    int totalVirtualNodes,
    Set<String> physicalNodes,
    Map<Long, String> ringSnapshot
) {}
