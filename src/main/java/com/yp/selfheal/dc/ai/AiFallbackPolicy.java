package com.yp.selfheal.dc.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiFallbackPolicy {
    private static final Logger log = LoggerFactory.getLogger(AiFallbackPolicy.class);

    public double fallbackFailureRisk(double phiSuspicion) {
        log.debug("Engaging deterministic fallback for failure assessment using Phi suspicion: {}", phiSuspicion);
        // Normalize phi (0..16) into risk probability 0.0..1.0
        return Math.min(1.0, Math.max(0.0, phiSuspicion / 14.0));
    }
}
