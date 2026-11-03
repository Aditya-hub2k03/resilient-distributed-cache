package com.yp.selfheal.dc.cluster;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhiAccrualFailureDetectorTest {

    private PhiAccrualFailureDetector detector;

    @BeforeEach
    void setUp() {
        detector = new PhiAccrualFailureDetector(10, 8.0);
    }

    @Test
    void testRegularHeartbeatsKeepPhiLow() {
        for (int i = 0; i < 5; i++) {
            detector.heartbeat("node-2");
        }
        double phi = detector.phi("node-2");
        assertTrue(phi < 8.0, "Phi should remain below suspect threshold with regular pings");
        assertFalse(detector.isSuspect("node-2"));
    }

    @Test
    void testProlongedSilenceAccruesPhi() throws InterruptedException {
        detector.heartbeat("node-2");
        Thread.sleep(150);
        detector.heartbeat("node-2");

        // Simulate 400ms silence (significantly longer than 150ms interval)
        Thread.sleep(400);

        double phi = detector.phi("node-2");
        assertTrue(phi > 0.0);
    }
}
