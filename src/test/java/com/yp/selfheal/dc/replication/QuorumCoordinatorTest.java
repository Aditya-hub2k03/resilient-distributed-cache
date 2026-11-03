package com.yp.selfheal.dc.replication;

import com.yp.selfheal.dc.exception.QuorumNotMetException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuorumCoordinatorTest {

    private QuorumCoordinator coordinator;

    @BeforeEach
    void setUp() {
        // N=3, W=2, R=2 (R + W = 4 > 3)
        coordinator = new QuorumCoordinator(3, 2, 2, new SimpleMeterRegistry());
    }

    @Test
    void testQuorumValidationPassesWhenAcksSufficient() {
        assertDoesNotThrow(() -> coordinator.validateWriteQuorum(2));
        assertDoesNotThrow(() -> coordinator.validateReadQuorum(2));
    }

    @Test
    void testWriteQuorumThrowsExceptionWhenAcksInsufficient() {
        assertThrows(QuorumNotMetException.class, () -> coordinator.validateWriteQuorum(1));
    }

    @Test
    void testInvalidQuorumConfigFails() {
        // R + W <= N should fail fast
        assertThrows(IllegalArgumentException.class, () ->
                new QuorumCoordinator(3, 1, 2, new SimpleMeterRegistry()));
    }
}
