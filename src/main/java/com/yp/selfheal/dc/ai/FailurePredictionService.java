package com.yp.selfheal.dc.ai;

import com.yp.selfheal.dc.cluster.PhiAccrualFailureDetector;
import com.yp.selfheal.dc.dto.FailurePredictionDto;
import com.yp.selfheal.dc.healing.QuarantineManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FailurePredictionService {
    private static final Logger log = LoggerFactory.getLogger(FailurePredictionService.class);

    private final SidecarClient sidecarClient;
    private final OllamaClient ollamaClient;
    private final PhiAccrualFailureDetector phiDetector;
    private final AiFallbackPolicy fallbackPolicy;
    private final QuarantineManager quarantineManager;
    private final MeterRegistry meterRegistry;

    @Value("${dc.ai.predictive-quarantine.threshold:0.80}")
    private double predictiveQuarantineThreshold;

    private final ConcurrentHashMap<String, AtomicLong> riskGauges = new ConcurrentHashMap<>();

    public FailurePredictionService(
            SidecarClient sidecarClient,
            OllamaClient ollamaClient,
            PhiAccrualFailureDetector phiDetector,
            AiFallbackPolicy fallbackPolicy,
            QuarantineManager quarantineManager,
            MeterRegistry meterRegistry) {
        this.sidecarClient = sidecarClient;
        this.ollamaClient = ollamaClient;
        this.phiDetector = phiDetector;
        this.fallbackPolicy = fallbackPolicy;
        this.quarantineManager = quarantineManager;
        this.meterRegistry = meterRegistry;
    }

    public FailurePredictionDto assessNode(String nodeId) {
        double phi = phiDetector.phi(nodeId);
        Map<String, Object> telemetry = Map.of("phi", phi, "timestamp", System.currentTimeMillis());

        Optional<Double> sidecarScore = sidecarClient.getFailureScore(nodeId, telemetry);
        boolean fallback = sidecarScore.isEmpty();
        double riskScore = sidecarScore.orElseGet(() -> fallbackPolicy.fallbackFailureRisk(phi));

        // Record risk metric
        riskGauges.computeIfAbsent(nodeId, id -> {
            AtomicLong gauge = new AtomicLong();
            meterRegistry.gauge("dc_ai_failure_risk", Tags.of("node", id), gauge);
            return gauge;
        }).set((long) (riskScore * 100));

        String riskLevel = riskScore >= 0.8 ? "CRITICAL" : (riskScore >= 0.5 ? "MEDIUM" : "LOW");
        String action = riskScore >= predictiveQuarantineThreshold ? "QUARANTINE" : (riskScore >= 0.5 ? "MONITOR" : "NONE");

        String prompt = String.format("Analyze node health. Node ID: %s, Failure Risk: %.2f, Suspicion Phi: %.2f. Suggest 1 sentence action.",
                nodeId, riskScore, phi);
        String explanation = ollamaClient.generate(prompt).orElse("Statistical baseline score evaluated.");

        if ("QUARANTINE".equals(action) && !quarantineManager.isQuarantined(nodeId)) {
            quarantineManager.quarantineNode(nodeId, "AI Predictive Failure detected with risk score: " + riskScore);
        }

        return new FailurePredictionDto(nodeId, riskScore, riskLevel, action, explanation, fallback);
    }
}
