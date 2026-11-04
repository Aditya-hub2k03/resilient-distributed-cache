package com.yp.selfheal.dc.ai;

import com.yp.selfheal.dc.cluster.PhiAccrualFailureDetector;
import com.yp.selfheal.dc.dto.FailurePredictionDto;
import com.yp.selfheal.dc.healing.QuarantineManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class FailurePredictionServiceTest {

    private SidecarClient sidecarClient;
    private OllamaClient ollamaClient;
    private PhiAccrualFailureDetector phiDetector;
    private QuarantineManager quarantineManager;
    private FailurePredictionService service;

    @BeforeEach
    void setUp() {
        sidecarClient = Mockito.mock(SidecarClient.class);
        ollamaClient = Mockito.mock(OllamaClient.class);
        phiDetector = Mockito.mock(PhiAccrualFailureDetector.class);
        quarantineManager = Mockito.mock(QuarantineManager.class);
        AiFallbackPolicy fallbackPolicy = new AiFallbackPolicy();

        service = new FailurePredictionService(
                sidecarClient,
                ollamaClient,
                phiDetector,
                fallbackPolicy,
                quarantineManager,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void testAssessNodeWithSidecarScore() {
        when(phiDetector.phi("node-2")).thenReturn(5.0);
        when(sidecarClient.getFailureScore(eq("node-2"), any())).thenReturn(Optional.of(0.85));
        when(ollamaClient.generate(any())).thenReturn(Optional.of("Node exhibits high packet loss."));

        FailurePredictionDto result = service.assessNode("node-2");

        assertEquals("node-2", result.nodeId());
        assertEquals(0.85, result.failureRiskScore());
        assertEquals("CRITICAL", result.riskLevel());
        assertEquals("QUARANTINE", result.suggestedAction());
        assertFalse(result.fallbackApplied());
    }

    @Test
    void testFallbackWhenSidecarUnavailable() {
        when(phiDetector.phi("node-2")).thenReturn(14.0);
        when(sidecarClient.getFailureScore(eq("node-2"), any())).thenReturn(Optional.empty());

        FailurePredictionDto result = service.assessNode("node-2");

        assertTrue(result.fallbackApplied());
        assertEquals(1.0, result.failureRiskScore());
        assertEquals("CRITICAL", result.riskLevel());
    }
}
