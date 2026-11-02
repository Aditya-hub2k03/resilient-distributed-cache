package com.yp.selfheal.dc.messaging;

import com.yp.selfheal.dc.config.KafkaConfig;
import com.yp.selfheal.dc.messaging.events.RebalanceStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    @KafkaListener(topics = KafkaConfig.TOPIC_REBALANCE, groupId = "dc-cluster-workers")
    public void handleRebalance(RebalanceStartedEvent event) {
        log.info("Received rebalance event on worker: id={}, action={}, triggeredBy={}",
                event.rebalanceId(), event.action(), event.triggerNodeId());
        // Worker synchronization & batch transfer placeholder
    }
}
