package com.yp.selfheal.dc.dto;

public record FailurePredictionDto(
    String nodeId,
    double failureRiskScore,
    String riskLevel, // LOW, MEDIUM, CRITICAL
    String suggestedAction, // MONITOR, QUARANTINE, DRAIN
    String llmExplanation,
    boolean fallbackApplied
) {}
