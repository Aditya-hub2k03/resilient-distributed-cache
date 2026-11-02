package com.yp.selfheal.dc.messaging;

import com.yp.selfheal.dc.config.KafkaConfig;
import com.yp.selfheal.dc.messaging.events.NodeJoinedEvent;
import com.yp.selfheal.dc.messaging.events.NodeLeftEvent;
import com.yp.selfheal.dc.messaging.events.RebalanceStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNodeJoined(NodeJoinedEvent event) {
        log.info("Broadcasting NodeJoinedEvent for node: {}", event.nodeId());
        kafkaTemplate.send(KafkaConfig.TOPIC_MEMBERSHIP, event.nodeId(), event);
    }

    public void sendNodeLeft(NodeLeftEvent event) {
        log.info("Broadcasting NodeLeftEvent for node: {}", event.nodeId());
        kafkaTemplate.send(KafkaConfig.TOPIC_MEMBERSHIP, event.nodeId(), event);
    }

    public void sendRebalanceEvent(RebalanceStartedEvent event) {
        log.info("Broadcasting RebalanceStartedEvent: id={}", event.rebalanceId());
        kafkaTemplate.send(KafkaConfig.TOPIC_REBALANCE, event.rebalanceId(), event);
    }
}
