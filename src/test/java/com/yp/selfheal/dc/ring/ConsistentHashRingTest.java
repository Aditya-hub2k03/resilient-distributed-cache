package com.yp.selfheal.dc.ring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConsistentHashRingTest {

    private ConsistentHashRing ring;

    @BeforeEach
    void setUp() {
        RingPartitioner partitioner = new RingPartitioner();
        ring = new ConsistentHashRing(partitioner, 20); // 20 virtual nodes per node
    }

    @Test
    void testNodeAdditionAndKeyRouting() {
        ring.addNode("node-A");
        ring.addNode("node-B");
        ring.addNode("node-C");

        assertEquals(3, ring.size());

        Optional<String> owner1 = ring.routeKey("user:1001");
        Optional<String> owner2 = ring.routeKey("order:9920");

        assertTrue(owner1.isPresent());
        assertTrue(owner2.isPresent());
        assertTrue(List.of("node-A", "node-B", "node-C").contains(owner1.get()));
    }

    @Test
    void testNodeRemovalRemapsKeys() {
        ring.addNode("node-A");
        ring.addNode("node-B");

        Optional<String> initialOwner = ring.routeKey("key-test");
        assertTrue(initialOwner.isPresent());

        // Remove the owner node
        ring.removeNode(initialOwner.get());
        assertEquals(1, ring.size());

        Optional<String> newOwner = ring.routeKey("key-test");
        assertTrue(newOwner.isPresent());
        assertNotEquals(initialOwner.get(), newOwner.get());
    }

    @Test
    void testSuccessorNodesForReplication() {
        ring.addNode("node-A");
        ring.addNode("node-B");
        ring.addNode("node-C");

        List<String> replicas = ring.getSuccessorNodes("key:replication", 2);
        assertEquals(2, replicas.size());
        assertNotEquals(replicas.get(0), replicas.get(1));
    }
}
