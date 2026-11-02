package com.yp.selfheal.dc.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfig {

    public static final String TOPIC_TELEMETRY = "dc.telemetry";
    public static final String TOPIC_MEMBERSHIP = "dc.membership";
    public static final String TOPIC_REBALANCE = "dc.rebalance";
    public static final String TOPIC_HANDOFF = "dc.handoff";
    public static final String TOPIC_ALERTS = "dc.alerts";

    @Bean
    public NewTopic telemetryTopic() {
        return TopicBuilder.name(TOPIC_TELEMETRY).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic membershipTopic() {
        return TopicBuilder.name(TOPIC_MEMBERSHIP).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic rebalanceTopic() {
        return TopicBuilder.name(TOPIC_REBALANCE).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic handoffTopic() {
        return TopicBuilder.name(TOPIC_HANDOFF).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic alertsTopic() {
        return TopicBuilder.name(TOPIC_ALERTS).partitions(3).replicas(1).build();
    }
}
