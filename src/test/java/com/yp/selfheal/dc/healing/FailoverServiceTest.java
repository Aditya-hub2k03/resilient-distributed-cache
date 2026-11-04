package com.yp.selfheal.dc.healing;

import com.yp.selfheal.dc.messaging.KafkaProducerService;
import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.model.NodeStatus;
import com.yp.selfheal.dc.repository.NodeRepository;
import com.yp.selfheal.dc.ring.ConsistentHashRing;
import com.yp.selfheal.dc.ring.RingPartitioner;
import com.yp.selfheal.dc.ring.RingRebalancer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class FailoverServiceTest {

    private ConsistentHashRing ring;
    private NodeRepository nodeRepository;
    private FailoverService failoverService;

    @BeforeEach
    void setUp() {
        ring = new ConsistentHashRing(new RingPartitioner(), 10);
        KafkaProducerService kafkaProducer = Mockito.mock(KafkaProducerService.class);
        RingRebalancer ringRebalancer = new RingRebalancer(ring, kafkaProducer, 100);
        nodeRepository = Mockito.mock(NodeRepository.class);

        failoverService = new FailoverService(ring, ringRebalancer, nodeRepository, new SimpleMeterRegistry());
    }

    @Test
    void testFailoverRemovesNodeFromRingAndSetsDead() {
        ring.addNode("node-dead");
        NodeRecord record = new NodeRecord("node-dead", "localhost", 8082, NodeStatus.ACTIVE);
        when(nodeRepository.findByNodeId("node-dead")).thenReturn(Optional.of(record));

        failoverService.failoverDeadNode("node-dead");

        assertFalse(ring.getPhysicalNodes().contains("node-dead"));
        verify(nodeRepository, times(1)).save(record);
    }
}
