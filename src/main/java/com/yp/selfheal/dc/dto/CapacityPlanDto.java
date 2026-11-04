package com.yp.selfheal.dc.dto;

public record CapacityPlanDto(
    int activeNodes,
    int totalKeys,
    double clusterUtilizationPercent,
    String scalingAdvice,
    String detailedReport
) {}
