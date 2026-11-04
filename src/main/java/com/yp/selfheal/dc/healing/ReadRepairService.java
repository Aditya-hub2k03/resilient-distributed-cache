package com.yp.selfheal.dc.healing;

import com.yp.selfheal.dc.cluster.NodeRegistry;
import com.yp.selfheal.dc.model.NodeRecord;
import com.yp.selfheal.dc.security.NodeAuthValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ReadRepairService {
    private static final Logger log = LoggerFactory.getLogger(ReadRepairService.class);

    private final NodeRegistry nodeRegistry;
    private final NodeAuthValidator nodeAuthValidator;
    private final RestTemplate restTemplate = new RestTemplate();

    public ReadRepairService(NodeRegistry nodeRegistry, NodeAuthValidator nodeAuthValidator) {
        this.nodeRegistry = nodeRegistry;
        this.nodeAuthValidator = nodeAuthValidator;
    }

    @Async
    public void dispatchRepair(String divergentNodeId, String key, Object authoritativeValue, Long ttlSeconds) {
        log.info("Dispatching asynchronous read repair for key '{}' to node '{}'", key, divergentNodeId);
        try {
            NodeRecord node = nodeRegistry.getNode(divergentNodeId).orElseThrow();
            String url = String.format("http://%s:%d/api/v1/cache/replica/put", node.getHost(), node.getPort());

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-DC-API-KEY", nodeAuthValidator.getClusterApiKey());
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = Map.of(
                    "key", key,
                    "value", authoritativeValue,
                    "ttlSeconds", ttlSeconds != null ? ttlSeconds : 0
            );

            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
            log.info("Read repair successfully synchronized key '{}' on node '{}'", key, divergentNodeId);
        } catch (Exception ex) {
            log.warn("Failed to apply read repair to node {}: {}", divergentNodeId, ex.getMessage());
        }
    }
}
